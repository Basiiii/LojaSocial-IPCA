const rateLimitStore = new Map();

const createRateLimit = (windowMs, maxRequests) => {
  return (req, res, next) => {
    const key = req.ip || 'unknown';
    const now = Date.now();
    const windowStart = now - windowMs;

    if (!rateLimitStore.has(key)) {
      rateLimitStore.set(key, []);
    }

    const requests = rateLimitStore.get(key);
    
    const validRequests = requests.filter(timestamp => timestamp > windowStart);
    rateLimitStore.set(key, validRequests);

    if (validRequests.length >= maxRequests) {
      return res.status(429).json({
        error: 'Too many requests',
        message: `Rate limit exceeded. Maximum ${maxRequests} requests per ${windowMs/1000} seconds.`
      });
    }

    validRequests.push(now);
    next();
  };
};

export { createRateLimit };
