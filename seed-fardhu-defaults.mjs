#!/usr/bin/env node
import fs from "node:fs/promises";
import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const SERVICE_ACCOUNT_PATH = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID;
const SEED_FILE = process.env.FARDHU_SEED_FILE || "./fardhu_defaults.seed.json";

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
    throw new Error("Seed file must be a JSON array.");
  }
  return parsed;
}

function validate(rows) {
  const timeRegex = /^\d{2}:\d{2}$/;
  if (rows.length !== 5) {
    throw new Error("Seed must contain exactly 5 fardhu rows.");
  }
  const ids = new Set();
  for (const row of rows) {
    if (!row.id || !row.name || !row.defaultTime) {
      throw new Error(`Invalid row: ${JSON.stringify(row)}`);
    }
    if (!timeRegex.test(row.defaultTime)) {
      throw new Error(`Invalid defaultTime format for ${row.id}: ${row.defaultTime}`);
    }
    if (ids.has(row.id)) {
      throw new Error(`Duplicate id: ${row.id}`);
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

  const rows = await loadJson(SEED_FILE);
  validate(rows);

  const db = getFirestore();
  const batch = db.batch();

  for (const row of rows) {
    const ref = db.collection("fardhu_defaults").doc(row.id);
    batch.set(
      ref,
      {
        name: row.name,
        defaultTime: row.defaultTime,
        icon: row.icon ?? "sun",
        order: row.order ?? 999,
        isSystem: true,
        updatedAt: new Date().toISOString()
      },
      { merge: true }
    );
  }

  await batch.commit();
  console.log("Seed completed: fardhu_defaults updated.");
}

main().catch((err) => {
  console.error("Seed failed:", err);
  process.exit(1);
});
