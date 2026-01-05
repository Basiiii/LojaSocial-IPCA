import express from 'express';
import axios from 'axios';
import logger from '../utils/logger.js';
import { validateAuth } from '../middleware/auth.js';
import { chatSystemPrompt } from '../config/prompts.js';

const router = express.Router();

router.post('/', async (req, res) => {
  logger.chat('Request received');
  
  if (!validateAuth(req)) {
    logger.chat('Unauthorized request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const requestBody = {
      ...req.body,
      messages: [chatSystemPrompt, ...req.body.messages]
    };

    logger.chat('Forwarding to OpenRouter');
    const response = await axios.post('https://openrouter.ai/api/v1/chat/completions', requestBody, {
      headers: {
        'Authorization': `Bearer ${process.env.OPENROUTER_API_KEY}`,
        'HTTP-Referer': 'https://lojasocial-ipca.app',
        'X-Title': 'Loja Social IPCA',
        'Content-Type': 'application/json'
      }
    });
    
    logger.chat('Response received', { status: response.status });
    res.json(response.data);
  } catch (error) {
    logger.error('Chat API error', error);
    res.status(error.response?.status || 500).json({
      error: 'Failed to process request',
      details: error.response?.data || error.message
    });
  }
});

export default router;
