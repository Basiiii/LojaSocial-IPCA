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

export default router;
