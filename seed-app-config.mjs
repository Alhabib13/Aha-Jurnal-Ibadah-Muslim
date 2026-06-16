#!/usr/bin/env node
/**
 * Seed app_config/feature_flags di Firestore.
 *
 * Dokumen ini dibaca oleh AdminConfigRepository untuk kontrol fitur global.
 * Menggunakan { merge: true } — field yang sudah ada di Firestore tidak ditimpa,
 * sehingga aman dijalankan ulang.
 *
 * Usage:
 *   FIREBASE_SERVICE_ACCOUNT_PATH=./secrets/sa.json \
 *   FIREBASE_PROJECT_ID=your-project-id \
 *   node seed-app-config.mjs
 */
import fs from "node:fs/promises";
import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const SERVICE_ACCOUNT_PATH = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID;

if (!SERVICE_ACCOUNT_PATH) {
  console.error("[seed-app-config] Missing FIREBASE_SERVICE_ACCOUNT_PATH env.");
  process.exit(1);
}
if (!PROJECT_ID) {
  console.error("[seed-app-config] Missing FIREBASE_PROJECT_ID env.");
  process.exit(1);
}

async function main() {
  const serviceAccountRaw = await fs.readFile(SERVICE_ACCOUNT_PATH, "utf8");
  const serviceAccount = JSON.parse(serviceAccountRaw);

  if (!getApps().length) {
    initializeApp({ credential: cert(serviceAccount), projectId: PROJECT_ID });
  }

  const db = getFirestore();

  // app_config/feature_flags — semua value default ke true
  // merge: true → tidak menimpa flag yang sudah diubah admin
  await db.collection("app_config").doc("feature_flags").set(
    {
      puasaWajibRamadanEnabled: true,
      ramadanScheduleByLocationEnabled: true,
      sholatTarawihEnabled: true,
      fardhuScheduleByLocationEnabled: true,
      seededAt: FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  console.log("[seed-app-config] app_config/feature_flags seeded (merge).");
}

main().catch((err) => {
  console.error("[seed-app-config] Seed failed:", err);
  process.exit(1);
});
