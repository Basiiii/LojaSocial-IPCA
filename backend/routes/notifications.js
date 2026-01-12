import express from 'express';
import logger from '../utils/logger.js';
import { validateAuth } from '../middleware/auth.js';
import {
  notifyNewApplication,
  notifyDateProposedOrAccepted,
  notifyBeneficiaryDateProposal,
  notifyNewRequest,
  notifyPickupReminder,
  notifyRequestAccepted,
  notifyApplicationAccepted,
  notifyApplicationRejected,
  notifyRequestRejected
} from '../services/notificationService.js';

const router = express.Router();

// POST /api/notifications/new-application
router.post('/new-application', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { applicationId } = req.body;
    if (!applicationId) {
      return res.status(400).json({ error: 'applicationId is required' });
    }

    const result = await notifyNewApplication(applicationId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in new application notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/date-proposed-or-accepted
router.post('/date-proposed-or-accepted', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId, recipientUserId, isAccepted } = req.body;
    if (!requestId || !recipientUserId) {
      return res.status(400).json({ error: 'requestId and recipientUserId are required' });
    }

    const result = await notifyDateProposedOrAccepted(requestId, recipientUserId, isAccepted || false);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in date proposed/accepted notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/beneficiary-date-proposal
router.post('/beneficiary-date-proposal', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId } = req.body;
    if (!requestId) {
      return res.status(400).json({ error: 'requestId is required' });
    }

    const result = await notifyBeneficiaryDateProposal(requestId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in beneficiary date proposal notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/new-request
router.post('/new-request', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId } = req.body;
    if (!requestId) {
      return res.status(400).json({ error: 'requestId is required' });
    }

    const result = await notifyNewRequest(requestId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in new request notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/pickup-reminder
router.post('/pickup-reminder', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId, beneficiaryUserId } = req.body;
    if (!requestId || !beneficiaryUserId) {
      return res.status(400).json({ error: 'requestId and beneficiaryUserId are required' });
    }

    const result = await notifyPickupReminder(requestId, beneficiaryUserId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in pickup reminder notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/request-accepted
router.post('/request-accepted', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId, beneficiaryUserId } = req.body;
    if (!requestId || !beneficiaryUserId) {
      return res.status(400).json({ error: 'requestId and beneficiaryUserId are required' });
    }

    const result = await notifyRequestAccepted(requestId, beneficiaryUserId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in request accepted notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/application-accepted
router.post('/application-accepted', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { applicationId, applicantUserId } = req.body;
    if (!applicationId || !applicantUserId) {
      return res.status(400).json({ error: 'applicationId and applicantUserId are required' });
    }

    const result = await notifyApplicationAccepted(applicationId, applicantUserId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in application accepted notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/application-rejected
router.post('/application-rejected', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { applicationId, applicantUserId } = req.body;
    if (!applicationId || !applicantUserId) {
      return res.status(400).json({ error: 'applicationId and applicantUserId are required' });
    }

    const result = await notifyApplicationRejected(applicationId, applicantUserId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in application rejected notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// POST /api/notifications/request-rejected
router.post('/request-rejected', async (req, res) => {
  if (!validateAuth(req)) {
    logger.error('Unauthorized notification request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const { requestId, beneficiaryUserId } = req.body;
    if (!requestId || !beneficiaryUserId) {
      return res.status(400).json({ error: 'requestId and beneficiaryUserId are required' });
    }

    const result = await notifyRequestRejected(requestId, beneficiaryUserId);
    res.json({
      success: result.success,
      message: result.success ? 'Notification sent' : 'Failed to send notification',
      error: result.error
    });
  } catch (error) {
    logger.error('Error in request rejected notification', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

export default router;
