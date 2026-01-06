// Ensure environment variables are loaded first
import './env.js';

import admin from 'firebase-admin';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { readFileSync } from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Initialize Firebase Admin (you may already have this)
if (!admin.apps.length) {
  // Try to load service account from environment variable or file
  let serviceAccount;
  
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    // If service account is provided as JSON string in environment variable
    try {
      serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    } catch (error) {
      // If JSON parsing fails, try base64 decoding (for production .env files)
      try {
        const decoded = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT, 'base64').toString('utf8');
        serviceAccount = JSON.parse(decoded);
      } catch (base64Error) {
        throw new Error('FIREBASE_SERVICE_ACCOUNT must be valid JSON or base64-encoded JSON');
      }
    }
  } else if (process.env.FIREBASE_SERVICE_ACCOUNT_PATH) {
    // If path to service account file is provided (best for production)
    const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
    serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));
  } else {
    // Try default path (for local development)
    try {
      const defaultPath = join(__dirname, '../admin-sdk.json');
      serviceAccount = JSON.parse(readFileSync(defaultPath, 'utf8'));
    } catch (error) {
      console.error('Firebase Admin: Service account not found.');
      console.error('Options:');
      console.error('  1. Set FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/service-account.json in .env (recommended for production)');
      console.error('  2. Set FIREBASE_SERVICE_ACCOUNT=<base64-encoded-json> in .env');
      console.error('  3. Place admin-sdk.json in backend root directory (for local dev)');
      throw new Error('Firebase service account configuration not found');
    }
  }
  
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();
const messaging = admin.messaging();

export { admin, db, messaging };
