// Load environment variables first
import './config/env.js';

import express from 'express';
import cors from 'cors';
import cron from 'node-cron';
import logger from './utils/logger.js';
import chatRoutes from './routes/chat.js';
import barcodeRoutes from './routes/barcode.js';
import expirationRoutes from './routes/expirationRoutes.js';
import auditRoutes from './routes/audit.js';
import imageRoutes from './routes/image.js';
import { checkAndNotifyExpiringItems } from './services/expirationCheckService.js';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

app.use('/api/chat', chatRoutes);
app.use('/api/barcode', barcodeRoutes);
app.use('/api/expiration', expirationRoutes);
app.use('/api/audit', auditRoutes);
app.use('/api/image', imageRoutes);

// Run daily at 9:00 AM
// Format: minute hour day month dayOfWeek
cron.schedule('0 9 * * *', async () => {
  logger.server('Running scheduled expiration check...');
  try {
    const result = await checkAndNotifyExpiringItems();
    logger.server(`Scheduled check completed: ${result.itemCount} expiring items, ${result.notificationsSent} notifications sent`);
  } catch (error) {
    logger.error('Scheduled expiration check failed', error);
  }
}, {
  scheduled: true,
  timezone: "Europe/Lisbon"
});

logger.server('Expiration check cron job scheduled for 9:00 AM daily');

app.listen(PORT, () => {
  logger.server(`API server running on port ${PORT}`);
});
