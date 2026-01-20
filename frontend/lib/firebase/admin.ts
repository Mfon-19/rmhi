import "server-only";

import { cert, getApps, initializeApp, App } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import fs from "fs";

let app: App;

function loadServiceAccount(): object {
  const json = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (json && json.trim()) {
    return JSON.parse(json);
  }

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (serviceAccountPath && serviceAccountPath.trim()) {
    return JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
  }

  throw new Error("Firebase admin credentials not configured.");
}

const serviceAccount = loadServiceAccount();

if (!getApps().length) {
  app = initializeApp({
    credential: cert(serviceAccount as object),
  });
} else {
  app = getApps()[0];
}

export const adminAuth = getAuth(app);
export default app;
