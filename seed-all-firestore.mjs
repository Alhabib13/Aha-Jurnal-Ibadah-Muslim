#!/usr/bin/env node
import { spawnSync } from "node:child_process";

const steps = [
  { label: "fardhu_defaults", file: "seed-fardhu-defaults.mjs" },
  { label: "islamic_content", file: "seed-islamic-content.mjs" }
];

for (const step of steps) {
  console.log(`\n[seed] Running ${step.label}...`);
  const result = spawnSync(process.execPath, [step.file], {
    stdio: "inherit",
    env: process.env
  });

  if (result.status !== 0) {
    console.error(`[seed] Failed at step: ${step.label}`);
    process.exit(result.status ?? 1);
  }
}

console.log("\n[seed] All Firestore seed steps completed.");
