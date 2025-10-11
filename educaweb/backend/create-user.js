const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: "postgresql://educaweb_db_user:XbmXQ6LSwLAzb4uWSnY1Qc3VGXktD33g@dpg-d3ke7549c44c73adh4k0-a/educaweb_db"
    }
  }
});

async function createUser() {
  try {
    console.log('🔗 Conectando ao banco...');
    
    // Hash da senha
    const hashedPassword = await bcrypt.hash('admin123', 12);
    console.log('🔐 Senha hash gerada');
    
    // Criar usuário
    const user = await prisma.user.create({
      data: {
        email: 'admin@educaweb.com',
        password: hashedPassword,
        name: 'Administrador',
        role: 'admin'
      }
    });
    
    console.log('✅ Usuário criado com sucesso!');
    console.log('📧 Email:', user.email);
    console.log('👤 Nome:', user.name);
    console.log('🔑 Role:', user.role);
    
  } catch (error) {
    console.error('❌ Erro ao criar usuário:', error);
  } finally {
    await prisma.$disconnect();
  }
}

createUser();
