const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Inicializando banco de dados...');
  
  // Criar usuário admin padrão
  const hashedPassword = await bcrypt.hash('admin123', 10);
  
  const admin = await prisma.user.upsert({
    where: { email: 'admin@educaweb.com' },
    update: {},
    create: {
      email: 'admin@educaweb.com',
      password: hashedPassword,
      nome: 'Administrador',
      role: 'admin'
    }
  });
  
  console.log('✅ Usuário admin criado:', admin.email);
  console.log('🔑 Senha padrão: admin123');
  
  // Criar dados de exemplo
  const disciplinas = [
    { nome: 'Matemática', descricao: 'Matemática básica e avançada' },
    { nome: 'Português', descricao: 'Língua portuguesa e literatura' },
    { nome: 'Ciências', descricao: 'Ciências naturais' },
    { nome: 'História', descricao: 'História do Brasil e mundial' },
    { nome: 'Geografia', descricao: 'Geografia física e humana' },
    { nome: 'Educação Física', descricao: 'Atividades físicas e esportes' },
    { nome: 'Artes', descricao: 'Artes visuais e expressão' }
  ];
  
  for (const disciplina of disciplinas) {
    await prisma.disciplina.upsert({
      where: { nome: disciplina.nome },
      update: {},
      create: disciplina
    });
  }
  
  console.log('✅ Disciplinas de exemplo criadas');
  
  // Criar salas de exemplo
  const salas = [
    { nome: 'Sala 1', tipo: 'Sala de Aula', capacidade: 30, recursos: 'Quadro, Projetor' },
    { nome: 'Sala 2', tipo: 'Sala de Aula', capacidade: 25, recursos: 'Quadro, TV' },
    { nome: 'Laboratório', tipo: 'Laboratório', capacidade: 20, recursos: 'Microscópios, Computadores' },
    { nome: 'Biblioteca', tipo: 'Biblioteca', capacidade: 40, recursos: 'Livros, Computadores' }
  ];
  
  for (const sala of salas) {
    await prisma.sala.upsert({
      where: { nome: sala.nome },
      update: {},
      create: sala
    });
  }
  
  console.log('✅ Salas de exemplo criadas');
  
  console.log('🎉 Banco de dados inicializado com sucesso!');
}

main()
  .catch((e) => {
    console.error('❌ Erro ao inicializar banco:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
