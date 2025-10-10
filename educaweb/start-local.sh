#!/bin/bash

echo "🎓 EducaWeb - Sistema de Gestão Educacional (Modo Local)"
echo "========================================================"

# Verificar Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js não encontrado. Instale o Node.js primeiro."
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "❌ npm não encontrado. Instale o npm primeiro."
    exit 1
fi

echo "✅ Node.js $(node --version) encontrado"
echo "✅ npm $(npm --version) encontrado"

# Configurar backend
echo ""
echo "🔧 Configurando backend..."

cd backend

# Instalar dependências se necessário
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependências do backend..."
    npm install
fi

# Configurar banco SQLite
echo "🗄️ Configurando banco de dados SQLite..."

# Atualizar schema para SQLite
cat > prisma/schema.sqlite.prisma << 'SCHEMA_EOF'
// This is your Prisma schema file,
// learn more about it in the docs: https://pris.ly/d/prisma-schema

generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlite"
  url      = "file:./dev.db"
}

model Usuario {
  id        Int      @id @default(autoincrement())
  nome      String
  email     String   @unique
  senha     String
  tipo      String   // DIRECAO, PROFESSOR, ALUNO
  avatar    String?
  ativo     Boolean  @default(true)
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  @@map("usuarios")
}

model Disciplina {
  id           Int      @id @default(autoincrement())
  nome         String
  descricao    String?
  cargaHoraria Int?
  requisitos   String   // JSON string
  createdAt    DateTime @default(now())
  updatedAt    DateTime @updatedAt

  @@map("disciplinas")
}

model Professor {
  id                Int      @id @default(autoincrement())
  nome              String
  email             String?  @unique
  disciplinas       String   // JSON string
  indisponibilidades String   // JSON string
  aulasContratadas  Int      @default(0)
  createdAt         DateTime @default(now())
  updatedAt         DateTime @updatedAt

  @@map("professores")
}

model Turma {
  id         Int      @id @default(autoincrement())
  nome       String
  serie      String?
  turno      String?  // MANHA, TARDE, NOITE
  capacidade Int?
  createdAt  DateTime @default(now())
  updatedAt  DateTime @updatedAt

  @@map("turmas")
}

model Sala {
  id          Int      @id @default(autoincrement())
  nome        String
  tipo        String   // SALA_AULA, LABORATORIO, BIBLIOTECA, AUDITORIO
  capacidade  Int?
  recursos    String   // JSON string
  observacoes String?
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  @@map("salas")
}

model GradeHoraria {
  id           Int      @id @default(autoincrement())
  turmaId      Int
  status       String   @default("RASCUNHO") // RASCUNHO, ATIVA, ARQUIVADA
  configuracao String   // JSON string
  createdAt    DateTime @default(now())
  updatedAt    DateTime @updatedAt

  @@map("grades_horarias")
}

model CelulaHorario {
  id                    String   @id @default(cuid())
  gradeId               Int
  dia                   String   // SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA
  horario               String
  disciplinaId          Int?
  professorId           Int?
  turmaId               Int?
  salaId                Int?
  temConflito           Boolean  @default(false)
  temConflitoDeSala     Boolean  @default(false)
  observacoes           String?

  @@map("celulas_horario")
}

model MensagemChat {
  id        String   @id @default(cuid())
  tipo      String   // USUARIO, ASSISTENTE
  conteudo  String
  metadata  String?  // JSON string
  createdAt DateTime @default(now())

  @@map("mensagens_chat")
}
SCHEMA_EOF

# Usar schema SQLite
cp prisma/schema.sqlite.prisma prisma/schema.prisma

# Configurar .env para SQLite
cat > .env << 'ENV_EOF'
DATABASE_URL="file:./dev.db"
JWT_SECRET=educaweb-super-secret-jwt-key-2024
PORT=3001
NODE_ENV=development
FRONTEND_URL=http://localhost:5173
GEMINI_API_KEY=your-gemini-api-key-here
ENV_EOF

# Gerar cliente Prisma
echo "�� Gerando cliente Prisma..."
npx prisma generate

# Executar migrações
echo "📊 Criando banco de dados..."
npx prisma migrate dev --name init

# Executar seed
echo "🌱 Populando banco com dados iniciais..."
npm run prisma:seed

echo "✅ Backend configurado!"

# Voltar para raiz e configurar frontend
cd ../frontend

echo ""
echo "🔧 Configurando frontend..."

# Instalar dependências se necessário
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependências do frontend..."
    npm install
fi

# Configurar .env do frontend
cat > .env << 'ENV_EOF'
VITE_API_URL=http://localhost:3001/api
VITE_SOCKET_URL=http://localhost:3001
VITE_GEMINI_API_KEY=your-gemini-api-key-here
VITE_APP_NAME=EducaWeb
VITE_APP_VERSION=1.0.0
ENV_EOF

echo "✅ Frontend configurado!"

# Voltar para raiz
cd ..

echo ""
echo "🚀 Iniciando EducaWeb..."

# Função para limpar processos ao sair
cleanup() {
    echo ""
    echo "🛑 Parando serviços..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    exit 0
}

# Capturar Ctrl+C
trap cleanup SIGINT

# Iniciar backend em background
echo "🔗 Iniciando backend na porta 3001..."
cd backend
npm run dev &
BACKEND_PID=$!
cd ..

# Aguardar backend iniciar
sleep 3

# Iniciar frontend em background
echo "🌐 Iniciando frontend na porta 5173..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ EducaWeb iniciado com sucesso!"
echo ""
echo "🌐 Frontend: http://localhost:5173"
echo "🔗 Backend API: http://localhost:3001/api"
echo ""
echo "👤 Usuário padrão:"
echo "   Email: admin@educaweb.com"
echo "   Senha: admin123"
echo ""
echo "📝 Para parar: Ctrl+C"
echo ""

# Aguardar processos
wait
