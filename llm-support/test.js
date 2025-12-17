import axios from 'axios';

const test = async () => {
  try {
    const response = await axios.post('http://localhost:3000/api/chat', {
      model: 'mistralai/devstral-2512:free',
      messages: [
        { role: 'user', content: 'Qual é o horário de funcionamento?' }
      ]
    }, {
      headers: {
        'Authorization': 'Bearer lojasocial2025'
      }
    });
    console.log('Response:', response.data.choices[0].message.content);
  } catch (error) {
    console.error('Error:', error.response?.data || error.message);
  }
};

test();