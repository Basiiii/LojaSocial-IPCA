import express from 'express';
import axios from 'axios';
import dotenv from 'dotenv';
import cors from 'cors';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.post('/api/chat', async (req, res) => {
  const authHeader = req.headers.authorization;
  const expectedPassword = process.env.API_PASSWORD;
  
  if (!authHeader || authHeader !== `Bearer ${expectedPassword}`) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const systemPrompt = {
      role: 'system',
      content: `Você é um assistente de suporte útil para a Loja Social IPCA. 
      
      O seu papel é responder APENAS sobre:
      - Gestão de inventário na Loja Social
      - Serviços para beneficiários e agendamentos
      - Procedimentos de doação
      - Disponibilidade de stock e alertas de expiração
      - Informações de contacto e horário de funcionamento
      
      FAQ:
      - P: Qual é o horário de funcionamento? R: Segunda-Sexta 9H-17H
      - P: Como agendar uma entrega? R: Vá para o ecrã inicial e clique em "Faz pedido"
      - P: Quais artigos estão disponíveis? R: Verifique a secção de inventário do aplicativo
      
      Se perguntado sobre qualquer coisa fora destes tópicos, redirecione educadamente: "Só posso ajudar com serviços da Loja Social. Para outras perguntas, por favor contacte o escritório principal."
      
      Mantenha respostas concisas e úteis.`
    };

    const requestBody = {
      ...req.body,
      messages: [systemPrompt, ...req.body.messages]
    };

    const response = await axios.post('https://openrouter.ai/api/v1/chat/completions', requestBody, {
      headers: {
        'Authorization': `Bearer ${process.env.OPENROUTER_API_KEY}`,
        'HTTP-Referer': 'https://lojasocial-ipca.app',
        'X-Title': 'Loja Social IPCA',
        'Content-Type': 'application/json'
      }
    });
    
    res.json(response.data);
  } catch (error) {
    console.error('Error forwarding to OpenRouter:', error.response?.data || error.message);
    res.status(error.response?.status || 500).json({
      error: 'Failed to process request',
      details: error.response?.data || error.message
    });
  }
});

app.get('/api/barcode', async (req, res) => {

  console.log('=== BARCODE API REQUEST ===');
  console.log('Request URL:', req.url);
  console.log('Request method:', req.method);
  console.log('Request headers:', req.headers);
  console.log('Request query params:', req.query);
  const apiUrl = process.env.BARCODE_API_URL;
  const apiKey = process.env.BARCODE_API_KEY;

  console.log('Environment variables:');
  console.log('- BARCODE_API_URL:', apiUrl ? 'SET' : 'NOT SET');
  console.log('- BARCODE_API_KEY:', apiKey ? 'SET' : 'NOT SET');
  
  if (!apiUrl || !apiKey) {
    console.log('ERROR: Barcode API configuration missing');
    return res.status(500).json({ error: 'Barcode API configuration missing' });
  }

  try {
    const { barcode } = req.query;
    console.log('Barcode parameter:', barcode);

    if (!barcode) {
      console.log('ERROR: Barcode parameter is required');
      return res.status(400).json({ error: 'Barcode parameter is required' });
    }
    console.log('Making external API call to:', `${apiUrl}/v3/products`);
    console.log('Request params:', {
      barcode: barcode,
      formatted: 'y',
      key: apiKey
    });
    const response = await axios.get(`${apiUrl}/v3/products`, {
      params: {
        barcode: barcode,
        formatted: 'y',
        key: apiKey
      }
    });

    console.log('External API response status:', response.status);
    console.log('External API response data:', JSON.stringify(response.data, null, 2));
    
    res.json(response.data);
  } catch (error) {
    console.error('Error fetching barcode data:', error.response?.data || error.message);
    console.error('Full error object:', error);
    res.status(error.response?.status || 500).json({
      error: 'Failed to fetch barcode data',
      details: error.response?.data || error.message
    });
  }
});

app.listen(PORT, () => {
  console.log(`LLM API server running on port ${PORT}`);
});