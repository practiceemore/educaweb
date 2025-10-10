# 🎓 EducaWeb - Execução Local (Sem Docker)

Guia para executar o EducaWeb sem Docker, usando Node.js diretamente.

## 🚀 Início Rápido

### Pré-requisitos
- ✅ Node.js 20+ (você já tem v20.16.0)
- ✅ npm 9+ (você já tem v9.2.0)

### Execução Automática

```bash
# Execute o script de inicialização
./start-local.sh
```

### Execução Manual

#### 1. Configurar Backend

```bash
cd backend

# Instalar dependências
npm install

# Configurar banco SQLite
cp prisma/schema.sqlite.prisma prisma/schema.prisma

# Configurar variáveis de ambiente
cat > .env << 'ENV_EOF'
DATABASE_URL="file:./dev.db"
JWT_SECRET=educaweb-super-secret-jwt-key-2024
PORT=3001
NODE_ENV=development
FRONTEND_URL=http://localhost:5173
GEMINI_API_KEY=your-gemini-api-key-here
ENV_EOF

# Gerar cliente Prisma
npx prisma generate

# Criar banco e executar migrações
npx prisma migrate dev --name init

# Popular banco com dados iniciais
npm run prisma:seed

# Iniciar backend
npm run dev
```

#### 2. Configurar Frontend (em outro terminal)

```bash
cd frontend

# Instalar dependências
npm install

# Configurar variáveis de ambiente
cat > .env << 'ENV_EOF'
VITE_API_URL=http://localhost:3001/api
VITE_SOCKET_URL=http://localhost:3001
VITE_GEMINI_API_KEY=your-gemini-api-key-here
VITE_APP_NAME=EducaWeb
VITE_APP_VERSION=1.0.0
ENV_EOF

# Iniciar frontend
npm run dev
```

## 🌐 Acesso

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:3001/api
- **Health Check**: http://localhost:3001/health

## 👤 Login Padrão

- **Email**: admin@educaweb.com
- **Senha**: admin123

## 🗄️ Banco de Dados

O sistema usa **SQLite** para execução local:
- Arquivo: `backend/dev.db`
- Visualizar: `npx prisma studio` (na pasta backend)

## 🛠️ Comandos Úteis

### Backend
```bash
cd backend

# Ver banco de dados
npx prisma studio

# Resetar banco
npx prisma migrate reset

# Gerar novo cliente
npx prisma generate

# Executar seed
npm run prisma:seed
```

### Frontend
```bash
cd frontend

# Build para produção
npm run build

# Preview da build
npm run preview
```

## 🔧 Solução de Problemas

### Erro de Porta em Uso
```bash
# Verificar processos na porta 3001
lsof -i :3001

# Verificar processos na porta 5173
lsof -i :5173

# Matar processo específico
kill -9 <PID>
```

### Erro de Dependências
```bash
# Limpar cache npm
npm cache clean --force

# Remover node_modules e reinstalar
rm -rf node_modules package-lock.json
npm install
```

### Erro de Banco de Dados
```bash
cd backend

# Deletar banco e recriar
rm -f dev.db
npx prisma migrate dev --name init
npm run prisma:seed
```

## 📁 Estrutura do Projeto

```
educaweb/
├── backend/                 # API Node.js
│   ├── src/                # Código fonte
│   ├── prisma/             # Schema e migrações
│   ├── dev.db              # Banco SQLite (criado automaticamente)
│   └── .env                # Variáveis de ambiente
├── frontend/               # App React
│   ├── src/                # Código fonte
│   └── .env                # Variáveis de ambiente
├── start-local.sh          # Script de inicialização
└── README-LOCAL.md         # Este arquivo
```

## 🎯 Próximos Passos

1. **Testar Login**: Acesse http://localhost:5173 e faça login
2. **Explorar Dashboard**: Navegue pelas funcionalidades
3. **Desenvolver**: Adicione novas funcionalidades
4. **Deploy**: Configure para produção quando necessário

## 🆘 Suporte

Se encontrar problemas:
1. Verifique se as portas 3001 e 5173 estão livres
2. Confirme se Node.js e npm estão atualizados
3. Execute `./start-local.sh` novamente
4. Verifique os logs no terminal

---

**EducaWeb** - Executando localmente sem Docker! 🚀
