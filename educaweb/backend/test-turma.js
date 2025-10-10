const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function testTurma() {
  try {
    const turma = await prisma.turma.create({
      data: {
        nome: "Turma Teste",
        serie: "1º Ano",
        turno: "Manhã",
        capacidade: 30,
        anoLetivo: "2024",
        userId: 1
      }
    });
    console.log("Turma criada:", turma);
  } catch (error) {
    console.error("Erro:", error);
  } finally {
    await prisma.$disconnect();
  }
}

testTurma();
