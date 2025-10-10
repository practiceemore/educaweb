// Configuração da API do Google Gemini
// Para obter sua chave da API:
// 1. Acesse: https://makersuite.google.com/app/apikey
// 2. Crie um novo projeto ou selecione um existente
// 3. Gere uma nova chave da API
// 4. Substitua a chave abaixo pela sua chave real

const GEMINI_API_CONFIG = {
    // SUBSTITUA PELA SUA CHAVE REAL DA API
    apiKey: 'SUA_CHAVE_DA_API_AQUI',
    
    // Configurações da API
    model: 'gemini-pro',
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent',
    
    // Configurações de geração
    generationConfig: {
        temperature: 0.7,        // Criatividade (0.0 - 1.0)
        topK: 40,               // Diversidade de tokens
        topP: 0.95,             // Probabilidade cumulativa
        maxOutputTokens: 1024,  // Tamanho máximo da resposta
    },
    
    // Configurações de segurança
    safetySettings: [
        {
            category: 'HARM_CATEGORY_HARASSMENT',
            threshold: 'BLOCK_MEDIUM_AND_ABOVE'
        },
        {
            category: 'HARM_CATEGORY_HATE_SPEECH',
            threshold: 'BLOCK_MEDIUM_AND_ABOVE'
        },
        {
            category: 'HARM_CATEGORY_SEXUALLY_EXPLICIT',
            threshold: 'BLOCK_MEDIUM_AND_ABOVE'
        },
        {
            category: 'HARM_CATEGORY_DANGEROUS_CONTENT',
            threshold: 'BLOCK_MEDIUM_AND_ABOVE'
        }
    ]
};

// Função para validar chave da API
function validateAPIKey(apiKey) {
    if (!apiKey || apiKey === 'SUA_CHAVE_DA_API_AQUI') {
        return false;
    }
    
    // Verificar formato básico da chave
    if (apiKey.length < 20 || !apiKey.startsWith('AIza')) {
        return false;
    }
    
    return true;
}

// Função para testar conexão com a API
async function testAPIConnection(apiKey) {
    try {
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                contents: [{
                    parts: [{
                        text: 'Teste de conexão'
                    }]
                }],
                generationConfig: GEMINI_API_CONFIG.generationConfig,
                safetySettings: GEMINI_API_CONFIG.safetySettings
            })
        });

        return response.ok;
    } catch (error) {
        console.error('Erro no teste da API:', error);
        return false;
    }
}

// Exportar configuração
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { GEMINI_API_CONFIG, validateAPIKey, testAPIConnection };
}
