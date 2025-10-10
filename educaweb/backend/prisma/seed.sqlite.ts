import { PrismaClient } from '@prisma/client';
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Iniciando seed do banco de dados multi-usuário...');

  // Criar usuário admin
  const hashedPassword = await bcrypt.hash('admin123', 12);
  
  const admin = await prisma.user.upsert({
    where: { email: 'admin@educaweb.com' },
    update: {},
    create: {
      email: 'admin@educaweb.com',
      password: hashedPassword,
      name: 'Administrador',
      role: 'admin'
    }
  });

  console.log('✅ Usuário admin criado:', admin.email);
  console.log('📝 Email: admin@educaweb.com');
  console.log('🔑 Senha: admin123');
  console.log('🎯 Cada usuário terá seus próprios dados isolados!');
  console.log('🎉 Seed concluído com sucesso!');
}

main()
  .catch((e) => {
    console.error('❌ Erro no seed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
