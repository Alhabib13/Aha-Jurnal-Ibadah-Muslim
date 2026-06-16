#!/usr/bin/env node
import fs from "node:fs/promises";
import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const SERVICE_ACCOUNT_PATH = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID;
const HADITH_SEED_FILE = process.env.HADITH_SEED_FILE || "./hadith_contents.seed.json";
const SURAH_SEED_FILE = process.env.SURAH_SEED_FILE || "./surah_verses.seed.json";

if (!SERVICE_ACCOUNT_PATH) {
  console.error("Missing FIREBASE_SERVICE_ACCOUNT_PATH env.");
  process.exit(1);
}

if (!PROJECT_ID) {
  console.error("Missing FIREBASE_PROJECT_ID env.");
  process.exit(1);
}

async function loadJson(path) {
  const raw = await fs.readFile(path, "utf8");
  const parsed = JSON.parse(raw);
  if (!Array.isArray(parsed)) {
    throw new Error(`Seed file must be a JSON array: ${path}`);
  }
  return parsed;
}

function validateHadith(rows) {
  if (rows.length === 0) {
    throw new Error("Hadith seed must contain at least one row.");
  }
  const ids = new Set();
  for (const row of rows) {
    if (!row.id || !row.text || !row.source) {
      throw new Error(`Invalid hadith row: ${JSON.stringify(row)}`);
    }
    if (ids.has(row.id)) {
      throw new Error(`Duplicate hadith id: ${row.id}`);
    }
    ids.add(row.id);
  }
}

function validateSurah(rows) {
  if (rows.length === 0) {
    throw new Error("Surah seed must contain at least one row.");
  }
  const ids = new Set();
  for (const row of rows) {
    const ayahNumber = Number(row.ayahNumber);
    if (!row.id || !row.surahName || !row.translation || !Number.isInteger(ayahNumber) || ayahNumber <= 0) {
      throw new Error(`Invalid surah row: ${JSON.stringify(row)}`);
    }
    if (ids.has(row.id)) {
      throw new Error(`Duplicate surah id: ${row.id}`);
    }
    ids.add(row.id);
  }
}

async function main() {
  const serviceAccountRaw = await fs.readFile(SERVICE_ACCOUNT_PATH, "utf8");
  const serviceAccount = JSON.parse(serviceAccountRaw);

  if (!getApps().length) {
    initializeApp({
      credential: cert(serviceAccount),
      projectId: PROJECT_ID
    });
  }

  const hadithRows = await loadJson(HADITH_SEED_FILE);
  const surahRows = await loadJson(SURAH_SEED_FILE);
  validateHadith(hadithRows);
  validateSurah(surahRows);

  const db = getFirestore();
  const batch = db.batch();
  const updatedAt = new Date().toISOString();

  for (const row of hadithRows) {
    const ref = db.collection("hadith_contents").doc(row.id);
    batch.set(
      ref,
      {
        text: row.text,
        source: row.source,
        updatedAt
      },
      { merge: true }
    );
  }

  for (const row of surahRows) {
    const ref = db.collection("surah_verses").doc(row.id);
    batch.set(
      ref,
      {
        surahName: row.surahName,
        ayahNumber: Number(row.ayahNumber),
        translation: row.translation,
        updatedAt
      },
      { merge: true }
    );
  }

  await batch.commit();
  console.log(
    `Seed completed: hadith_contents=${hadithRows.length}, surah_verses=${surahRows.length}`
  );
}

main().catch((err) => {
  console.error("Seed failed:", err);
  process.exit(1);
});
