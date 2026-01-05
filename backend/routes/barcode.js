import express from 'express';
import axios from 'axios';
import logger from '../utils/logger.js';
import { createRateLimit } from '../middleware/rateLimit.js';
import { createCache } from '../middleware/cache.js';

const router = express.Router();

const barcodeRateLimit = createRateLimit(1000, 1);
const barcodeCache = createCache(10 * 60 * 1000);

router.use('/', barcodeRateLimit);
router.use('/', barcodeCache);

router.get('/', async (req, res) => {
  logger.barcode('Request received', { barcode: req.query.barcode });
  
  const apiUrl = process.env.BARCODE_API_URL;
  const apiKey = process.env.BARCODE_API_KEY;
  
  if (!apiUrl || !apiKey) {
    logger.barcode('Configuration missing');
    return res.status(500).json({ error: 'Barcode API configuration missing' });
  }

  try {
    const { barcode } = req.query;

    if (!barcode) {
      logger.barcode('Missing barcode parameter');
      return res.status(400).json({ error: 'Barcode parameter is required' });
    }

    logger.barcode('Fetching product data', { barcode });
    const response = await axios.get(`${apiUrl}/v3/products`, {
      params: {
        barcode: barcode,
        formatted: 'y',
        key: apiKey
      }
    });

    logger.barcode('Response received', { 
      status: response.status,
      hasData: !!response.data,
      productCount: response.data?.products?.length || 0
    });
    
    res.json(response.data);
  } catch (error) {
    logger.error('Barcode API error', error);
    res.status(error.response?.status || 500).json({
      error: 'Failed to fetch barcode data',
      details: error.response?.data || error.message
    });
  }
});

export default router;
