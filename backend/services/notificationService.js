import { db, messaging, admin } from '../config/firebaseAdmin.js';
import logger from '../utils/logger.js';

/**
 * Get FCM token for a user by userId
 * @param {string} userId - User ID
 * @returns {Promise<string|null>} - FCM token or null if not found
 */
async function getUserFcmToken(userId) {
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      logger.server(`User ${userId} not found`);
      return null;
    }
    const data = userDoc.data();
    return data.fcmToken || null;
  } catch (error) {
    logger.error(`Error getting FCM token for user ${userId}`, error);
    return null;
  }
}

/**
 * Get all admin users with FCM tokens
 * @returns {Promise<Array>} - Array of admin users with FCM tokens
 */
async function getAdminUsersWithTokens() {
  try {
    const usersRef = db.collection('users');
    const snapshot = await usersRef
      .where('isAdmin', '==', true)
      .get();
    
    const adminUsers = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      if (data.fcmToken) {
        adminUsers.push({
          uid: doc.id,
          fcmToken: data.fcmToken,
          name: data.name || 'Admin',
          email: data.email || ''
        });
      }
    });
    
    logger.server(`Found ${adminUsers.length} admin users with FCM tokens`);
    return adminUsers;
  } catch (error) {
    logger.error('Error getting admin users', error);
    throw error;
  }
}

/**
 * Send FCM notification to a specific user
 * @param {string} fcmToken - FCM token of the recipient
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {object} data - Additional data payload
 * @returns {Promise<{success: boolean, error?: string}>}
 */
async function sendNotificationToUser(fcmToken, title, body, data = {}) {
  try {
    if (!fcmToken) {
      logger.server('No FCM token provided, skipping notification');
      return { success: false, error: 'No FCM token' };
    }

    const message = {
      notification: {
        title,
        body,
      },
      data: {
        ...data,
        type: data.type || 'general',
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'default',
          sound: 'default',
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1,
          }
        }
      },
      token: fcmToken
    };

    const response = await messaging.send(message);
    logger.server(`Successfully sent notification: ${title} to token ${fcmToken.substring(0, 20)}...`);
    return { success: true, messageId: response };
  } catch (error) {
    logger.error(`Error sending notification: ${error.message}`, error);
    // If token is invalid, log it
    if (error.code === 'messaging/invalid-registration-token' || 
        error.code === 'messaging/registration-token-not-registered') {
      logger.server(`Invalid token, should be removed from Firestore`);
    }
    return { success: false, error: error.message };
  }
}

/**
 * Send notification to multiple users
 * @param {Array<string>} fcmTokens - Array of FCM tokens
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {object} data - Additional data payload
 * @returns {Promise<{successCount: number, failureCount: number, results: Array}>}
 */
async function sendNotificationToUsers(fcmTokens, title, body, data = {}) {
  const results = await Promise.all(
    fcmTokens.map(token => sendNotificationToUser(token, title, body, data))
  );
  
  const successCount = results.filter(r => r.success).length;
  const failureCount = results.filter(r => !r.success).length;
  
  logger.server(`Sent ${successCount}/${fcmTokens.length} notifications successfully`);
  
  return {
    successCount,
    failureCount,
    results
  };
}

/**
 * Send notification for new application
 * @param {string} applicationId - Application ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyNewApplication(applicationId) {
  try {
    const adminUsers = await getAdminUsersWithTokens();
    if (adminUsers.length === 0) {
      logger.server('No admin users with FCM tokens found for new application notification');
      return { success: false };
    }

    const tokens = adminUsers.map(u => u.fcmToken);
    const result = await sendNotificationToUsers(
      tokens,
      'Nova Candidatura',
      'Uma nova candidatura foi submetida',
      {
        type: 'new_application',
        screen: 'applicationDetail',
        applicationId: applicationId
      }
    );

    return { success: result.successCount > 0 };
  } catch (error) {
    logger.error('Error sending new application notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification for new date proposed/accepted
 * @param {string} requestId - Request ID
 * @param {string} recipientUserId - User ID of the recipient (employee or beneficiary)
 * @param {boolean} isAccepted - Whether the date was accepted (true) or just proposed (false)
 * @returns {Promise<{success: boolean}>}
 */
async function notifyDateProposedOrAccepted(requestId, recipientUserId, isAccepted = false) {
  try {
    const fcmToken = await getUserFcmToken(recipientUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for user ${recipientUserId}`);
      return { success: false };
    }

    const title = isAccepted ? 'Nova Data Aceite' : 'Nova Data Proposta';
    const body = isAccepted 
      ? 'Uma nova data de levantamento foi aceite'
      : 'Uma nova data de levantamento foi proposta';

    const result = await sendNotificationToUser(
      fcmToken,
      title,
      body,
      {
        type: 'date_proposed_or_accepted',
        screen: 'requestDetails',
        requestId: requestId
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending date proposed/accepted notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification for new request
 * @param {string} requestId - Request ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyNewRequest(requestId) {
  try {
    const adminUsers = await getAdminUsersWithTokens();
    if (adminUsers.length === 0) {
      logger.server('No admin users with FCM tokens found for new request notification');
      return { success: false };
    }

    const tokens = adminUsers.map(u => u.fcmToken);
    const result = await sendNotificationToUsers(
      tokens,
      'Novo Pedido',
      'Um novo pedido foi submetido',
      {
        type: 'new_request',
        screen: 'requestDetails',
        requestId: requestId
      }
    );

    return { success: result.successCount > 0 };
  } catch (error) {
    logger.error('Error sending new request notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send pickup reminder notification
 * @param {string} requestId - Request ID
 * @param {string} beneficiaryUserId - Beneficiary user ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyPickupReminder(requestId, beneficiaryUserId) {
  try {
    const fcmToken = await getUserFcmToken(beneficiaryUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for beneficiary ${beneficiaryUserId}`);
      return { success: false };
    }

    const result = await sendNotificationToUser(
      fcmToken,
      'Lembrete de Levantamento',
      'Tens um levantamento agendado para hoje',
      {
        type: 'pickup_reminder',
        screen: 'requestDetails',
        requestId: requestId
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending pickup reminder notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification when request is accepted
 * @param {string} requestId - Request ID
 * @param {string} beneficiaryUserId - Beneficiary user ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyRequestAccepted(requestId, beneficiaryUserId) {
  try {
    const fcmToken = await getUserFcmToken(beneficiaryUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for beneficiary ${beneficiaryUserId}`);
      return { success: false };
    }

    const result = await sendNotificationToUser(
      fcmToken,
      'Pedido Aceite',
      'O teu pedido foi aceite',
      {
        type: 'request_accepted',
        screen: 'requestDetails',
        requestId: requestId
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending request accepted notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification when application is accepted
 * @param {string} applicationId - Application ID
 * @param {string} applicantUserId - Applicant user ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyApplicationAccepted(applicationId, applicantUserId) {
  try {
    const fcmToken = await getUserFcmToken(applicantUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for applicant ${applicantUserId}`);
      return { success: false };
    }

    const result = await sendNotificationToUser(
      fcmToken,
      'Candidatura Aceite',
      'A tua candidatura foi aceite',
      {
        type: 'application_accepted',
        screen: 'beneficiaryPortal'
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending application accepted notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification when application is rejected
 * @param {string} applicationId - Application ID
 * @param {string} applicantUserId - Applicant user ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyApplicationRejected(applicationId, applicantUserId) {
  try {
    const fcmToken = await getUserFcmToken(applicantUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for applicant ${applicantUserId}`);
      return { success: false };
    }

    const result = await sendNotificationToUser(
      fcmToken,
      'Candidatura Rejeitada',
      'A tua candidatura foi rejeitada',
      {
        type: 'application_rejected',
        screen: 'applicationDetail',
        applicationId: applicationId
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending application rejected notification', error);
    return { success: false, error: error.message };
  }
}

/**
 * Send notification when request is rejected
 * @param {string} requestId - Request ID
 * @param {string} beneficiaryUserId - Beneficiary user ID
 * @returns {Promise<{success: boolean}>}
 */
async function notifyRequestRejected(requestId, beneficiaryUserId) {
  try {
    const fcmToken = await getUserFcmToken(beneficiaryUserId);
    if (!fcmToken) {
      logger.server(`No FCM token found for beneficiary ${beneficiaryUserId}`);
      return { success: false };
    }

    const result = await sendNotificationToUser(
      fcmToken,
      'Pedido Rejeitado',
      'O teu pedido foi rejeitado',
      {
        type: 'request_rejected',
        screen: 'requestDetails',
        requestId: requestId
      }
    );

    return result;
  } catch (error) {
    logger.error('Error sending request rejected notification', error);
    return { success: false, error: error.message };
  }
}

export {
  sendNotificationToUser,
  sendNotificationToUsers,
  notifyNewApplication,
  notifyDateProposedOrAccepted,
  notifyNewRequest,
  notifyPickupReminder,
  notifyRequestAccepted,
  notifyApplicationAccepted,
  notifyApplicationRejected,
  notifyRequestRejected,
  getUserFcmToken,
  getAdminUsersWithTokens
};
