import express from 'express';
import { checkAndNotifyExpiringItems } from '../services/expirationCheckService.js';
import logger from '../utils/logger.js';

const router = express.Router();

// Manual trigger endpoint (optional, for testing)
router.post('/check-expiring', async (req, res) => {
  try {
    await checkAndNotifyExpiringItems();
    res.json({ success: true, message: 'Expiration check completed' });
  } catch (error) {
    logger.error('Error in manual expiration check', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

export default router;
