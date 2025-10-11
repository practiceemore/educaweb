const { PrismaClient } = require('@prisma/client');

// Usar a mesma configuração do backend deployado
const prisma = new PrismaClient();

async function testConnection() {
  try {
    console.log('🔗 Testando conexão com o banco...');
    
    // Testar conexão simples
    await prisma.$connect();
    console.log('✅ Conexão estabelecida!');
    
    // Verificar se a tabela User existe
    const userCount = await prisma.user.count();
    console.log('📊 Total de usuários:', userCount);
    
    // Listar usuários existentes
    const users = await prisma.user.findMany({
      select: {
        id: true,
        email: true,
        name: true,
        role: true
      }
    });
    
    console.log('👥 Usuários existentes:', users);
    
  } catch (error) {
    console.error('❌ Erro na conexão:', error.message);
    console.error('🔍 Detalhes:', error);
  } finally {
    await prisma.$disconnect();
  }
}

testConnection();
