import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';
import { createServer } from 'http';
import { Server } from 'socket.io';
import { PrismaClient } from '@prisma/client';

// Importar rotas
import authRoutes from './routes/auth';
import disciplinaRoutes from './routes/disciplinas';
import professorRoutes from './routes/professores';
import turmaRoutes from './routes/turmas';
import salaRoutes from './routes/salas';
import chatRoutes from './routes/chat';
import gradeRoutes from './routes/grades';
import turmaDisciplinaRoutes from './routes/turma-disciplinas';
import gradeHorariaRoutes from './routes/grade-horaria';
import setupRoutes from './routes/setup';
import relatoriosRoutes from './routes/relatorios';

// Importar middleware
import { errorHandler } from './middleware/errorHandler';
import { authMiddleware } from './middleware/auth';

// Configurar variáveis de ambiente
dotenv.config();

// Inicializar Prisma
const prisma = new PrismaClient();
export { prisma };

// Criar aplicação Express
const app = express();
const server = createServer(app);

// Configurar Socket.IO
const io = new Server(server, {
  cors: {
    origin: process.env.FRONTEND_URL || "http://localhost:5173",
    methods: ["GET", "POST"]
  }
});

// Middleware global
app.use(helmet());
app.use(cors({
  origin: process.env.FRONTEND_URL || "http://localhost:5173",
  credentials: true
}));
app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Rota de health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    version: '1.0.0'
  });
});

// Rotas da API
app.use('/api/auth', authRoutes);
app.use('/api/setup', setupRoutes);
app.use('/api/disciplinas', authMiddleware, disciplinaRoutes);
app.use('/api/professores', authMiddleware, professorRoutes);
app.use('/api/turmas', authMiddleware, turmaRoutes);
app.use('/api/salas', authMiddleware, salaRoutes);
app.use('/api/chat', authMiddleware, chatRoutes);
app.use('/api/grades', authMiddleware, gradeRoutes);
app.use('/api/turma-disciplinas', authMiddleware, turmaDisciplinaRoutes);
app.use('/api/grade-horaria', authMiddleware, gradeHorariaRoutes);
app.use('/api/relatorios', authMiddleware, relatoriosRoutes);

// Middleware de tratamento de erros
app.use(errorHandler);

// Socket.IO para comunicação em tempo real
io.on('connection', (socket) => {
  console.log('Cliente conectado:', socket.id);

  socket.on('disconnect', () => {
    console.log('Cliente desconectado:', socket.id);
  });

  // Eventos específicos do EducaWeb
  socket.on('join-room', (room) => {
    socket.join(room);
    console.log(`Cliente ${socket.id} entrou na sala ${room}`);
  });

  socket.on('leave-room', (room) => {
    socket.leave(room);
    console.log(`Cliente ${socket.id} saiu da sala ${room}`);
  });
});

// Exportar io para uso em outras partes da aplicação
export { io };

// Inicializar servidor
const PORT = process.env.PORT || 3001;

server.listen(PORT, () => {
  console.log(`🚀 Servidor rodando na porta ${PORT}`);
  console.log(`📊 Health check: http://localhost:${PORT}/health`);
  console.log(`🔗 API base: http://localhost:${PORT}/api`);
});

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('🛑 Encerrando servidor...');
  await prisma.$disconnect();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('🛑 Encerrando servidor...');
  await prisma.$disconnect();
  process.exit(0);
});

// Endpoint de teste para debug
app.post('/test', (req, res) => {
  console.log('Test endpoint - Body:', req.body);
  console.log('Test endpoint - Headers:', req.headers);
  res.json({ body: req.body, headers: req.headers });
});
