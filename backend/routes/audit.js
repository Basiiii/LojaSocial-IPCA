import express from 'express';
import logger from '../utils/logger.js';
import { validateAuth } from '../middleware/auth.js';
import { db, admin } from '../config/firebaseAdmin.js';

const router = express.Router();

// Valid action types
const VALID_ACTIONS = [
  'add_item',
  'remove_item',
  'accept_request',
  'decline_request',
  'accept_application',
  'decline_application'
];

// POST /api/audit/log - Log a single action
router.post('/log', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized audit log request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { action, userId, details } = req.body;

    // Validate action type
    if (!action || !VALID_ACTIONS.includes(action)) {
      return res.status(400).json({ 
        error: 'Invalid action type',
        validActions: VALID_ACTIONS
      });
    }

    // Create audit log document
    const auditLog = {
      action,
      timestamp: admin.firestore.Timestamp.now(),
      userId: userId || null,
      details: details || null
    };

    // Store in Firestore
    await db.collection('audit_logs').add(auditLog);

    logger.server(`Audit log created: ${action}`, { userId, hasDetails: !!details });
    
    res.status(201).json({ 
      success: true,
      message: 'Audit log created successfully'
    });
  } catch (error) {
    logger.error('Audit log creation failed', error);
    res.status(500).json({
      error: 'Failed to create audit log',
      details: error.message
    });
  }
});

// GET /api/audit/logs - Retrieve audit logs between dates
router.get('/logs', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized audit logs retrieval request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { startDate, endDate } = req.query;

    if (!startDate || !endDate) {
      return res.status(400).json({ 
        error: 'Both startDate and endDate query parameters are required (ISO 8601 format)'
      });
    }

    // Parse dates
    let startTimestamp, endTimestamp;
    try {
      startTimestamp = admin.firestore.Timestamp.fromDate(new Date(startDate));
      endTimestamp = admin.firestore.Timestamp.fromDate(new Date(endDate));
    } catch (error) {
      return res.status(400).json({ 
        error: 'Invalid date format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00Z)'
      });
    }

    // Query Firestore for logs in date range
    const logsSnapshot = await db.collection('audit_logs')
      .where('timestamp', '>=', startTimestamp)
      .where('timestamp', '<=', endTimestamp)
      .orderBy('timestamp', 'desc')
      .get();

    // Convert Firestore documents to JSON
    const logs = logsSnapshot.docs.map(doc => {
      const data = doc.data();
      return {
        id: doc.id,
        action: data.action,
        timestamp: data.timestamp.toDate().toISOString(),
        userId: data.userId,
        details: data.details
      };
    });

    logger.server(`Retrieved ${logs.length} audit logs`, { startDate, endDate });
    
    res.json({
      success: true,
      count: logs.length,
      logs
    });
  } catch (error) {
    logger.error('Audit logs retrieval failed', error);
    res.status(500).json({
      error: 'Failed to retrieve audit logs',
      details: error.message
    });
  }
});


// GET /api/audit/campaign/:campaignId/products - Retrieve all products received for a campaign
router.get('/campaign/:campaignId/products', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized campaign products retrieval request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { campaignId } = req.params;

    if (!campaignId) {
      return res.status(400).json({ 
        error: 'Campaign ID is required'
      });
    }

    logger.server(`Fetching campaign products for campaignId: ${campaignId}`);

    // Query audit_logs collection for campaign_receive_product actions with this campaignId
    let snapshot;
    try {
      snapshot = await db.collection('audit_logs')
        .where('action', '==', 'campaign_receive_product')
        .where('campaignId', '==', campaignId)
        .orderBy('timestamp', 'desc')
        .get();
    } catch (error) {
      // If orderBy fails (index missing), try without ordering
      logger.server(`OrderBy failed, fetching without order: ${error.message}`);
      snapshot = await db.collection('audit_logs')
        .where('action', '==', 'campaign_receive_product')
        .where('campaignId', '==', campaignId)
        .get();
      
      // Sort manually by timestamp descending
      const docs = snapshot.docs.sort((a, b) => {
        const aTime = a.data().timestamp?.toMillis?.() || 0;
        const bTime = b.data().timestamp?.toMillis?.() || 0;
        return bTime - aTime;
      });
      snapshot = { docs };
    }

    logger.server(`Found ${snapshot.docs.length} product receipts for campaign`);

    // Process each receipt and fetch product information
    const receipts = await Promise.all(
      snapshot.docs.map(async (doc) => {
        try {
          const data = doc.data();
          const itemId = data.itemId;
          const quantity = data.quantity || 0;
          const barcode = data.barcode;
          const userId = data.userId || null;
          
          // Convert timestamp
          let timestamp = null;
          if (data.timestamp) {
            if (data.timestamp.toDate) {
              timestamp = data.timestamp.toDate().toISOString();
            } else if (data.timestamp instanceof Date) {
              timestamp = data.timestamp.toISOString();
            }
          }

          // Fetch product information from products collection
          let product = null;
          if (barcode) {
            try {
              const productDoc = await db.collection('products').doc(barcode).get();
              if (productDoc.exists) {
                const productData = productDoc.data();
                product = {
                  id: productDoc.id,
                  name: productData.name || '',
                  brand: productData.brand || '',
                  category: productData.category || 1,
                  imageUrl: productData.imageUrl || ''
                };
              }
            } catch (error) {
              logger.error(`Error fetching product for barcode ${barcode}: ${error.message}`);
            }
          }

          return {
            itemId,
            quantity,
            barcode,
            timestamp,
            userId,
            product
          };
        } catch (error) {
          logger.error(`Error parsing audit log document ${doc.id}: ${error.message}`);
          return null;
        }
      })
    );

    // Filter out null results
    const validReceipts = receipts.filter(receipt => receipt !== null);

    logger.server(`Successfully loaded ${validReceipts.length} campaign products`);
    
    res.json({
      success: true,
      count: validReceipts.length,
      products: validReceipts
    });
  } catch (error) {
    logger.error('Campaign products retrieval failed', error);
    res.status(500).json({
      error: 'Failed to retrieve campaign products',
      details: error.message
    });
  }
});

export default router;
