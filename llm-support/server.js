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

app.listen(PORT, () => {
  console.log(`LLM API server running on port ${PORT}`);
});