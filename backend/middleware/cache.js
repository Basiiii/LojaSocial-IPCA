import logger from '../utils/logger.js';

const cacheStore = new Map();

const createCache = (ttlMs) => {
  return (req, res, next) => {
    const key = req.originalUrl;
    const now = Date.now();
    
    if (cacheStore.has(key)) {
      const cached = cacheStore.get(key);
      
      if (now - cached.timestamp < ttlMs) {
        logger.barcode('Cache hit', { key, age: now - cached.timestamp });
        return res.json(cached.data);
      } else {
        cacheStore.delete(key);
      }
    }
    
    const originalJson = res.json;
    res.json = function(data) {
      cacheStore.set(key, {
        data: data,
        timestamp: now
      });
      logger.barcode('Cache set', { key });
      return originalJson.call(this, data);
    };
    
    next();
  };
};

const clearCache = () => {
  cacheStore.clear();
  logger.server('Cache cleared');
};

export { createCache, clearCache };
