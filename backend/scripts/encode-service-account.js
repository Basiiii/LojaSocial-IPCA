#!/usr/bin/env node
/**
 * Helper script to encode Firebase service account JSON to base64
 * Usage: node scripts/encode-service-account.js <path-to-service-account.json>
 * 
 * This will output a base64-encoded string that you can paste into .env as:
 * FIREBASE_SERVICE_ACCOUNT=<output>
 */

import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const serviceAccountPath = process.argv[2];

if (!serviceAccountPath) {
  console.error('Usage: node scripts/encode-service-account.js <path-to-service-account.json>');
  process.exit(1);
}

try {
  const serviceAccount = readFileSync(resolve(serviceAccountPath), 'utf8');
  // Validate it's valid JSON
  JSON.parse(serviceAccount);
  // Encode to base64
  const encoded = Buffer.from(serviceAccount).toString('base64');
  console.log('\n✅ Base64-encoded service account:');
  console.log('─'.repeat(80));
  console.log(encoded);
  console.log('─'.repeat(80));
  console.log('\n📝 Add this to your .env file:');
  console.log(`FIREBASE_SERVICE_ACCOUNT=${encoded}\n`);
} catch (error) {
  console.error('❌ Error:', error.message);
  process.exit(1);
}
