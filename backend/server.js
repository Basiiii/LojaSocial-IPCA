import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import logger from './utils/logger.js';
import chatRoutes from './routes/chat.js';
import barcodeRoutes from './routes/barcode.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.use('/api/chat', chatRoutes);
app.use('/api/barcode', barcodeRoutes);

app.listen(PORT, () => {
  logger.server(`API server running on port ${PORT}`);
});
