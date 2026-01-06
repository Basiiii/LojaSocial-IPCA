// Load environment variables first
import './config/env.js';

import express from 'express';
import cors from 'cors';
import cron from 'node-cron';
import logger from './utils/logger.js';
import chatRoutes from './routes/chat.js';
import barcodeRoutes from './routes/barcode.js';
import expirationRoutes from './routes/expirationRoutes.js';
import { checkAndNotifyExpiringItems } from './services/expirationCheckService.js';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.use('/api/chat', chatRoutes);
app.use('/api/barcode', barcodeRoutes);
app.use('/api/expiration', expirationRoutes);

// Run daily at 9:00 AM
// Format: minute hour day month dayOfWeek
cron.schedule('0 9 * * *', async () => {
  logger.server('Running scheduled expiration check...');
  await checkAndNotifyExpiringItems();
}, {
  scheduled: true,
  timezone: "Europe/Lisbon"
});

logger.server('Expiration check cron job scheduled for 9:00 AM daily');

app.listen(PORT, () => {
  logger.server(`API server running on port ${PORT}`);
});
