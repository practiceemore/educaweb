# 🎓 EducaWeb - Sistema de Gestão Escolar com IA

Sistema completo de gestão escolar com geração automática de grade horária usando Inteligência Artificial.

## 🚀 Funcionalidades

- **👥 Gestão de Professores** - Cadastro e gerenciamento
- **📚 Gestão de Disciplinas** - Matérias e conteúdos
- **🏫 Gestão de Salas** - Ambientes e recursos
- **👨‍🎓 Gestão de Turmas** - Classes e séries
- **📅 Grade Horária** - Geração automática com IA
- **🤖 Chat com IA** - Assistente inteligente para gestão

## 🛠️ Tecnologias

- **Frontend**: HTML, CSS, JavaScript (Vite)
- **Backend**: Node.js, Express, TypeScript
- **Banco**: PostgreSQL (Prisma ORM)
- **IA**: Google Gemini 2.5 Pro

## 📦 Instalação

```bash
# Instalar dependências
npm run install-all

# Configurar banco de dados
cd backend
npx prisma db push
npx prisma generate

# Executar em desenvolvimento
npm run dev
```

## 🌐 Deploy

O sistema está configurado para deploy automático no Render.com

## 📝 Variáveis de Ambiente

```env
DATABASE_URL="postgresql://..."
JWT_SECRET="seu-jwt-secret"
GEMINI_API_KEY="sua-chave-gemini"
GEMINI_MODEL="gemini-2.5-pro"
FRONTEND_URL="https://seu-frontend.onrender.com"
```

## 🎯 Uso

1. Acesse o sistema
2. Faça login com suas credenciais
3. Configure professores, disciplinas, salas e turmas
4. Use o chat com IA para gerar grade horária automaticamente
5. Visualize e gerencie a grade horária gerada

## 📞 Suporte

Para suporte técnico, entre em contato através do sistema de chat integrado.