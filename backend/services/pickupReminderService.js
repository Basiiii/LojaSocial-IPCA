import { db, admin } from '../config/firebaseAdmin.js';
import logger from '../utils/logger.js';
import { notifyPickupReminder } from './notificationService.js';

/**
 * Check for pickups scheduled for today and send reminder notifications
 * @returns {Promise<{success: boolean, remindersSent: number}>}
 */
async function checkAndSendPickupReminders() {
  try {
    logger.server('Starting pickup reminder check...');
    
    const now = new Date();
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);
    
    const endOfToday = new Date(now);
    endOfToday.setHours(23, 59, 59, 999);
    
    logger.server(`Checking for pickups scheduled between ${startOfToday.toISOString()} and ${endOfToday.toISOString()}`);
    
    // Query requests with status PENDENTE_LEVANTAMENTO (1) and scheduledPickupDate today
    const requestsRef = db.collection('requests');
    const snapshot = await requestsRef
      .where('status', '==', 1) // PENDENTE_LEVANTAMENTO
      .where('scheduledPickupDate', '>=', admin.firestore.Timestamp.fromDate(startOfToday))
      .where('scheduledPickupDate', '<=', admin.firestore.Timestamp.fromDate(endOfToday))
      .get();
    
    logger.server(`Found ${snapshot.size} pickups scheduled for today`);
    
    const reminderResults = await Promise.all(
      snapshot.docs.map(async (doc) => {
        const data = doc.data();
        const beneficiaryUserId = data.userId;
        const requestId = doc.id;
        
        if (beneficiaryUserId) {
          try {
            const result = await notifyPickupReminder(requestId, beneficiaryUserId);
            return { success: result.success, requestId, beneficiaryUserId };
          } catch (error) {
            logger.error(`Error sending reminder for request ${requestId}`, error);
            return { success: false, requestId, beneficiaryUserId, error: error.message };
          }
        } else {
            logger.server(`Request ${requestId} has no userId, skipping reminder`);
            return { success: false, requestId, error: 'No userId' };
        }
      })
    );
    
    const successCount = reminderResults.filter(r => r.success).length;
    logger.server(`Sent ${successCount}/${reminderResults.length} pickup reminders successfully`);
    
    return {
      success: true,
      remindersSent: successCount,
      totalPickups: snapshot.size
    };
  } catch (error) {
    logger.error('Error in pickup reminder check', error);
    throw error;
  }
}

export {
  checkAndSendPickupReminders
};
