import { db, messaging, admin } from '../config/firebaseAdmin.js';
import logger from '../utils/logger.js';

/**
 * Check for items expiring within the specified number of days
 * @param {number} daysThreshold - Number of days to check ahead (default: 3)
 * @returns {Promise<number>} - Count of expiring items
 */
async function checkExpiringItems(daysThreshold = 3) {
  try {
    const now = new Date();
    const thresholdDate = new Date();
    thresholdDate.setDate(now.getDate() + daysThreshold);
    
    // Query items collection
    // Filter: quantity > 0 AND expirationDate exists AND expirationDate <= thresholdDate
    const itemsRef = db.collection('items');
    
    const snapshot = await itemsRef
      .where('quantity', '>', 0)
      .where('expirationDate', '<=', admin.firestore.Timestamp.fromDate(thresholdDate))
      .get();
    
    // Filter out items without expirationDate (shouldn't happen with the query, but safety check)
    const expiringItems = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      if (data.expirationDate) {
        const expDate = data.expirationDate.toDate();
        if (expDate <= thresholdDate && expDate >= now) {
          expiringItems.push({
            id: doc.id,
            ...data
          });
        }
      }
    });
    
    logger.server(`Found ${expiringItems.length} items expiring within ${daysThreshold} days`);
    return expiringItems.length;
  } catch (error) {
    logger.error('Error checking expiring items', error);
    throw error;
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
      // Check if user has FCM token stored
      // Token might be stored as fcmToken field or in a subcollection
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
 * Send FCM notification to admin users about expiring items
 * @param {number} itemCount - Number of expiring items
 */
async function sendExpirationNotifications(itemCount) {
  try {
    if (itemCount === 0) {
      logger.server('No expiring items, skipping notifications');
      return;
    }
    
    const adminUsers = await getAdminUsersWithTokens();
    
    if (adminUsers.length === 0) {
      logger.server('No admin users with FCM tokens found');
      return;
    }
    
    const message = {
      notification: {
        title: 'Aviso de Validade',
        body: `${itemCount} ${itemCount === 1 ? 'item está' : 'itens estão'} próximo${itemCount === 1 ? '' : 's'} do prazo de validade`,
      },
      data: {
        type: 'expiring_items',
        itemCount: itemCount.toString(),
        screen: 'expiringItems', // Deep link to expiring items screen
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'stock_warnings',
          sound: 'default',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK', // Adjust if needed for your app
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: itemCount,
          }
        }
      }
    };
    
    // Send to each admin user
    const sendPromises = adminUsers.map(user => {
      return messaging.send({
        ...message,
        token: user.fcmToken
      })
      .then(response => {
        logger.server(`Successfully sent notification to ${user.name} (${user.uid}): ${response}`);
        return { success: true, uid: user.uid };
      })
      .catch(error => {
        logger.error(`Error sending notification to ${user.name} (${user.uid})`, error);
        // If token is invalid, you might want to remove it from Firestore
        if (error.code === 'messaging/invalid-registration-token' || 
            error.code === 'messaging/registration-token-not-registered') {
          logger.server(`Invalid token for user ${user.uid}, should be removed from Firestore`);
          // Optionally: remove invalid token from Firestore
          // db.collection('users').doc(user.uid).update({ fcmToken: admin.firestore.FieldValue.delete() });
        }
        return { success: false, uid: user.uid, error: error.message };
      });
    });
    
    const results = await Promise.all(sendPromises);
    const successCount = results.filter(r => r.success).length;
    logger.server(`Sent ${successCount}/${adminUsers.length} notifications successfully`);
    
    return results;
  } catch (error) {
    logger.error('Error sending expiration notifications', error);
    throw error;
  }
}

/**
 * Main function to check expiring items and send notifications
 * @returns {Promise<{success: boolean, itemCount?: number, notificationsSent?: number, error?: string}>}
 */
async function checkAndNotifyExpiringItems() {
  try {
    logger.server('Starting expiration check...');
    const itemCount = await checkExpiringItems(3); // 3 days threshold
    const notificationResults = await sendExpirationNotifications(itemCount);
    
    // notificationResults can be undefined if no items or no admin users
    const notificationsSent = (notificationResults && Array.isArray(notificationResults))
      ? notificationResults.filter(r => r.success).length 
      : 0;
    
    logger.server(`Expiration check completed: ${itemCount} expiring items, ${notificationsSent} notifications sent`);
    
    return {
      success: true,
      itemCount,
      notificationsSent,
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error('Error in expiration check process', error);
    // Re-throw so the caller can handle it
    throw error;
  }
}

export {
  checkExpiringItems,
  getAdminUsersWithTokens,
  sendExpirationNotifications,
  checkAndNotifyExpiringItems
};
