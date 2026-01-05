const logger = {
  chat: (message, data = null) => {
    console.log(`[CHAT API] ${message}`, data ? JSON.stringify(data, null, 2) : '');
  },
  barcode: (message, data = null) => {
    console.log(`[BARCODE API] ${message}`, data ? JSON.stringify(data, null, 2) : '');
  },
  error: (message, error = null) => {
    console.error(`[ERROR] ${message}`, error ? error.message || error : '');
  },
  server: (message) => {
    console.log(`[SERVER] ${message}`);
  }
};

export default logger;
