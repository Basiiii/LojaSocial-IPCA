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
import notificationRoutes from './routes/notifications.js';
import { checkAndNotifyExpiringItems } from './services/expirationCheckService.js';
import { checkAndSendPickupReminders } from './services/pickupReminderService.js';

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
app.use('/api/notifications', notificationRoutes);

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

// Run pickup reminders daily at 8:00 AM
cron.schedule('0 8 * * *', async () => {
  logger.server('Running scheduled pickup reminder check...');
  try {
    const result = await checkAndSendPickupReminders();
    logger.server(`Scheduled pickup reminder check completed: ${result.remindersSent}/${result.totalPickups} reminders sent`);
  } catch (error) {
    logger.error('Scheduled pickup reminder check failed', error);
  }
}, {
  scheduled: true,
  timezone: "Europe/Lisbon"
});

logger.server('Pickup reminder cron job scheduled for 8:00 AM daily');

app.listen(PORT, () => {
  logger.server(`API server running on port ${PORT}`);
});
