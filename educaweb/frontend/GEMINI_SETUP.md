# 🤖 Configuração da API do Google Gemini

## 📋 Pré-requisitos

1. **Conta Google**: Você precisa de uma conta Google ativa
2. **Acesso ao Google AI Studio**: Acesse [makersuite.google.com](https://makersuite.google.com)
3. **Projeto Google Cloud**: Crie ou selecione um projeto

## 🔑 Como Obter sua Chave da API

### Passo 1: Acessar o Google AI Studio
1. Vá para [https://makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)
2. Faça login com sua conta Google

### Passo 2: Criar/Selecionar Projeto
1. Se você não tem um projeto, clique em "Create Project"
2. Dê um nome ao projeto (ex: "EducaWeb-IA")
3. Selecione o projeto criado

### Passo 3: Gerar Chave da API
1. Clique em "Create API Key"
2. Escolha o projeto criado
3. Copie a chave gerada (formato: `AIzaSy...`)

### Passo 4: Configurar no EducaWeb
1. Abra o EducaWeb no navegador
2. Vá para a aba "Assistente IA"
3. Clique no botão "🔑 Configurar API"
4. Cole sua chave da API
5. Clique em "OK"

## ⚙️ Configurações Avançadas

### Personalizar Comportamento da IA
Você pode modificar as configurações em `gemini-config.js`:

```javascript
generationConfig: {
    temperature: 0.7,        // Criatividade (0.0 = conservador, 1.0 = criativo)
    topK: 40,               // Diversidade de respostas
    topP: 0.95,             // Probabilidade cumulativa
    maxOutputTokens: 1024,  // Tamanho máximo da resposta
}
```

### Configurações de Segurança
O sistema já inclui configurações de segurança para:
- Bloquear conteúdo ofensivo
- Filtrar discurso de ódio
- Prevenir conteúdo sexualmente explícito
- Bloquear conteúdo perigoso

## 🚀 Funcionalidades da IA

### Análises Inteligentes
- **Grade Horária**: Otimização automática e detecção de conflitos
- **Professores**: Análise de eficiência e distribuição de carga
- **Turmas**: Análise de ocupação e distribuição
- **Disciplinas**: Utilização e planejamento

### Sugestões Avançadas
- **Otimização Inteligente**: Melhorias específicas para sua escola
- **Análise de Performance**: Relatórios detalhados de desempenho
- **Planejamento Estratégico**: Planos para o próximo semestre
- **Gestão de Recursos**: Otimização de salas e professores

## 🔧 Solução de Problemas

### Erro: "API Key inválida"
- Verifique se a chave foi copiada corretamente
- Certifique-se de que a chave começa com "AIza"
- Verifique se o projeto está ativo no Google Cloud

### Erro: "Quota exceeded"
- Você atingiu o limite de requisições
- Aguarde 24 horas ou atualize seu plano
- Verifique seu uso no Google Cloud Console

### Erro: "Network error"
- Verifique sua conexão com a internet
- Tente novamente em alguns minutos
- Verifique se não há firewall bloqueando

## 📊 Limites da API

### Gratuito (Padrão)
- **Requisições**: 15 por minuto
- **Tokens**: 32.000 por minuto
- **Dados**: 1 milhão de tokens por mês

### Pago (Pay-as-you-go)
- **Requisições**: 360 por minuto
- **Tokens**: 1 milhão por minuto
- **Dados**: Sem limite mensal

## 🛡️ Segurança

### Proteção da Chave
- **Nunca** compartilhe sua chave da API
- **Nunca** commite a chave em repositórios públicos
- Use variáveis de ambiente em produção
- Monitore o uso no Google Cloud Console

### Dados Sensíveis
- A IA não armazena dados pessoais
- Todas as comunicações são criptografadas
- Dados são processados apenas para análise
- Nenhum dado é compartilhado com terceiros

## 📞 Suporte

### Problemas Técnicos
- Verifique os logs do navegador (F12)
- Teste a conexão com a API
- Verifique as configurações de segurança

### Limites e Quotas
- Acesse o [Google Cloud Console](https://console.cloud.google.com)
- Vá para "APIs & Services" > "Quotas"
- Monitore seu uso em tempo real

## 🎯 Próximos Passos

1. **Configure sua chave da API**
2. **Teste a conexão** na aba "Assistente IA"
3. **Explore as funcionalidades** disponíveis
4. **Personalize as configurações** conforme necessário
5. **Monitore o uso** para otimizar custos

---

**🎓 EducaWeb - Sistema de Gerenciamento Educacional com IA**
