const { PrismaClient } = require('@prisma/client');

// Usar a mesma configuração do backend deployado
const prisma = new PrismaClient();

async function applySchema() {
  try {
    console.log('🔗 Conectando ao banco...');
    
    // Testar conexão
    await prisma.$connect();
    console.log('✅ Conexão estabelecida!');
    
    // Aplicar schema (criar tabelas)
    console.log('📊 Aplicando schema...');
    await prisma.$executeRaw`CREATE TABLE IF NOT EXISTS "users" (
      "id" SERIAL PRIMARY KEY,
      "email" VARCHAR(255) UNIQUE NOT NULL,
      "password" VARCHAR(255) NOT NULL,
      "name" VARCHAR(255) NOT NULL,
      "role" VARCHAR(255) DEFAULT 'admin',
      "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )`;
    
    console.log('✅ Tabela users criada!');
    
    // Criar usuário admin
    const bcrypt = require('bcryptjs');
    const hashedPassword = await bcrypt.hash('admin123', 12);
    
    const user = await prisma.user.create({
      data: {
        email: 'admin@educaweb.com',
        password: hashedPassword,
        name: 'Administrador',
        role: 'admin'
      }
    });
    
    console.log('✅ Usuário admin criado!');
    console.log('📧 Email:', user.email);
    console.log('👤 Nome:', user.name);
    
  } catch (error) {
    console.error('❌ Erro:', error.message);
  } finally {
    await prisma.$disconnect();
  }
}

applySchema();
