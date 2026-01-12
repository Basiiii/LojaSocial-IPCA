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
  'decline_application',
  'create_urgent_request',
  'urgent_request_item'
];

// POST /api/audit/log - Log a single action
router.post('/log', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized audit log request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { action, userId, userName, details } = req.body;

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
      userName: (userId && userName) ? userName : null,
      details: details || null
    };

    // Store in Firestore
    await db.collection('audit_logs').add(auditLog);

    // Enhanced logging for urgent requests
    if (action === 'create_urgent_request') {
      const requestId = details?.requestId || 'unknown';
      const beneficiaryUserId = details?.beneficiaryUserId || 'unknown';
      const itemsCount = details?.itemsCount || 0;
      logger.server(`[URGENT REQUEST] Created by user ${userId} for beneficiary ${beneficiaryUserId} - RequestId: ${requestId}, Items: ${itemsCount}`);
    } else if (action === 'urgent_request_item') {
      const itemId = details?.itemId || 'unknown';
      const barcode = details?.barcode || 'unknown';
      const quantity = details?.quantity || 0;
      const productName = details?.productName || 'unknown';
      const requestId = details?.requestId || 'unknown';
      logger.server(`[URGENT REQUEST ITEM] Removed: ${productName} (${barcode}) - Quantity: ${quantity}, ItemId: ${itemId}, RequestId: ${requestId}`);
    } else {
      logger.server(`Audit log created: ${action} by user ${userId || 'unknown'}`);
    }
    
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
    // For logs without userName, try to fetch it from users collection
    const logs = await Promise.all(
      logsSnapshot.docs.map(async (doc) => {
        const data = doc.data();
        let userName = data.userName || null;
        
        // If we have userId but no userName, try to fetch it
        if (data.userId && !userName) {
          try {
            const userDoc = await db.collection('users').doc(data.userId).get();
            if (userDoc.exists) {
              const userData = userDoc.data();
              userName = userData.name || null;
            }
          } catch (error) {
            logger.error(`Error fetching user name for userId ${data.userId}: ${error.message}`);
          }
        }
        
        return {
          id: doc.id,
          action: data.action,
          timestamp: data.timestamp.toDate().toISOString(),
          userId: data.userId,
          userName: userName,
          details: data.details || {}
        };
      })
    );

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

    // First, get the campaign name from the campaign ID
    let campaignName = null;
    try {
      const campaignDoc = await db.collection('campaigns').doc(campaignId).get();
      if (campaignDoc.exists) {
        const campaignData = campaignDoc.data();
        campaignName = campaignData.name || null;
        logger.server(`Found campaign name: ${campaignName} for campaignId: ${campaignId}`);
      } else {
        logger.server(`Campaign not found for campaignId: ${campaignId}`);
      }
    } catch (error) {
      logger.error(`Error fetching campaign name: ${error.message}`);
    }

    if (!campaignName) {
      logger.server(`No campaign name found, returning empty results`);
      return res.json({
        success: true,
        count: 0,
        products: []
      });
    }

    let snapshot;
    try {
      // Try with orderBy first
      let addItemSnapshot;
      try {
        addItemSnapshot = await db.collection('audit_logs')
          .where('action', '==', 'add_item')
          .orderBy('timestamp', 'desc')
          .get();
      } catch (orderByError) {
        // If orderBy fails (index missing), try without ordering
        logger.server(`OrderBy failed, fetching without order: ${orderByError.message}`);
        addItemSnapshot = await db.collection('audit_logs')
          .where('action', '==', 'add_item')
          .get();
      }
      
      logger.server(`Found ${addItemSnapshot.docs.length} total add_item logs`);
      
      // Filter client-side for campaignName in details.campaignId (which actually stores the name)
      const campaignAddItems = addItemSnapshot.docs.filter(doc => {
        const data = doc.data();
        const details = data.details || {};
        // The campaignId field in details actually contains the campaign name
        const hasCampaignMatch = details.campaignId === campaignName;
        
        // Debug logging for first few matches
        if (hasCampaignMatch) {
          logger.server(`Found campaign product: itemId=${details.itemId}, quantity=${details.quantity}, barcode=${details.barcode}`);
        }
        
        return hasCampaignMatch;
      });
      
      logger.server(`Filtered to ${campaignAddItems.length} campaign products for campaignName: ${campaignName}`);
      
      // Sort manually if we didn't use orderBy
      if (!addItemSnapshot.docs[0]?.data()?.timestamp?.toMillis) {
        campaignAddItems.sort((a, b) => {
          const aData = a.data();
          const bData = b.data();
          const aTime = aData.timestamp?.toMillis?.() || (aData.timestamp?.toDate?.()?.getTime() || 0);
          const bTime = bData.timestamp?.toMillis?.() || (bData.timestamp?.toDate?.()?.getTime() || 0);
          return bTime - aTime; // Descending order
        });
      }
      
      snapshot = { docs: campaignAddItems };
    } catch (error) {
      logger.server(`Error fetching campaign products: ${error.message}`);
      logger.error('Campaign products query error', error);
      snapshot = { docs: [] };
    }

    logger.server(`Found ${snapshot.docs.length} product receipts for campaign`);

    // Process each receipt and fetch product information
    const receipts = await Promise.all(
      snapshot.docs.map(async (doc) => {
        try {
          const data = doc.data();
          
          // Extract data from details (add_item with campaignId in details)
          const details = data.details || {};
          const itemId = details.itemId || '';
          const quantity = details.quantity || 0;
          const barcode = details.barcode || '';
          const userId = data.userId || null;
          
          // Debug: log if we're missing required fields
          if (!barcode) {
            logger.server(`Warning: Campaign product receipt missing barcode. itemId=${itemId}, quantity=${quantity}`);
          }
          
          // Fetch user name from users collection if userId is available
          let userName = data.userName || null;
          if (!userName && userId) {
            try {
              const userDoc = await db.collection('users').doc(userId).get();
              if (userDoc.exists) {
                const userData = userDoc.data();
                userName = userData.name || null;
              }
            } catch (error) {
              logger.error(`Error fetching user name for userId ${userId}: ${error.message}`);
            }
          }
          
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
            userName,
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

    logger.server(`Successfully loaded ${validReceipts.length} campaign products (from ${receipts.length} total receipts)`);
    
    // Log sample of receipts for debugging
    if (validReceipts.length > 0) {
      logger.server(`Sample receipt: ${JSON.stringify(validReceipts[0])}`);
    } else {
      logger.server(`No valid receipts found. Check if campaignId matches in details.campaignId`);
    }
    
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
