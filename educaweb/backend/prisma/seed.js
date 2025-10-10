"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const prisma = new client_1.PrismaClient();
async function main() {
    console.log('🌱 Iniciando seed do banco de dados...');
    const hashedPassword = await bcryptjs_1.default.hash('admin123', 10);
    const admin = await prisma.usuario.upsert({
        where: { email: 'admin@educaweb.com' },
        update: {},
        create: {
            nome: 'Administrador',
            email: 'admin@educaweb.com',
            senha: hashedPassword,
            tipo: 'DIRECAO',
            ativo: true
        }
    });
    console.log('✅ Usuário administrador criado:', admin.email);
    const disciplinas = [
        { nome: 'Matemática', descricao: 'Matemática Básica', cargaHoraria: 4 },
        { nome: 'Português', descricao: 'Língua Portuguesa', cargaHoraria: 4 },
        { nome: 'História', descricao: 'História do Brasil', cargaHoraria: 2 },
        { nome: 'Geografia', descricao: 'Geografia Geral', cargaHoraria: 2 },
        { nome: 'Ciências', descricao: 'Ciências Naturais', cargaHoraria: 3 },
        { nome: 'Educação Física', descricao: 'Atividades Físicas', cargaHoraria: 2 },
        { nome: 'Artes', descricao: 'Artes Visuais', cargaHoraria: 1 },
        { nome: 'Inglês', descricao: 'Língua Inglesa', cargaHoraria: 2 }
    ];
    for (const disciplina of disciplinas) {
        await prisma.disciplina.upsert({
            where: { nome: disciplina.nome },
            update: {},
            create: disciplina
        });
    }
    console.log('✅ Disciplinas criadas');
    const professores = [
        {
            nome: 'João Silva',
            email: 'joao@escola.com',
            disciplinas: ['Matemática'],
            aulasContratadas: 20
        },
        {
            nome: 'Maria Santos',
            email: 'maria@escola.com',
            disciplinas: ['Português'],
            aulasContratadas: 20
        },
        {
            nome: 'Pedro Costa',
            email: 'pedro@escola.com',
            disciplinas: ['História', 'Geografia'],
            aulasContratadas: 16
        },
        {
            nome: 'Ana Oliveira',
            email: 'ana@escola.com',
            disciplinas: ['Ciências'],
            aulasContratadas: 12
        },
        {
            nome: 'Carlos Lima',
            email: 'carlos@escola.com',
            disciplinas: ['Educação Física'],
            aulasContratadas: 10
        }
    ];
    for (const professor of professores) {
        await prisma.professor.upsert({
            where: { email: professor.email },
            update: {},
            create: professor
        });
    }
    console.log('✅ Professores criados');
    const turmas = [
        { nome: '6º Ano A', serie: '6º Ano', turno: 'MANHA', capacidade: 30 },
        { nome: '6º Ano B', serie: '6º Ano', turno: 'TARDE', capacidade: 28 },
        { nome: '7º Ano A', serie: '7º Ano', turno: 'MANHA', capacidade: 32 },
        { nome: '7º Ano B', serie: '7º Ano', turno: 'TARDE', capacidade: 30 },
        { nome: '8º Ano A', serie: '8º Ano', turno: 'MANHA', capacidade: 29 }
    ];
    for (const turma of turmas) {
        await prisma.turma.upsert({
            where: { nome: turma.nome },
            update: {},
            create: turma
        });
    }
    console.log('✅ Turmas criadas');
    const salas = [
        { nome: 'Sala 101', tipo: 'SALA_AULA', capacidade: 30, recursos: ['Projetor', 'Ar condicionado'] },
        { nome: 'Sala 102', tipo: 'SALA_AULA', capacidade: 30, recursos: ['Projetor'] },
        { nome: 'Sala 103', tipo: 'SALA_AULA', capacidade: 25, recursos: ['Ar condicionado'] },
        { nome: 'Laboratório de Ciências', tipo: 'LABORATORIO', capacidade: 20, recursos: ['Microscópios', 'Materiais de laboratório'] },
        { nome: 'Sala de Artes', tipo: 'SALA_AULA', capacidade: 20, recursos: ['Cavaletes', 'Materiais artísticos'] },
        { nome: 'Quadra', tipo: 'AUDITORIO', capacidade: 100, recursos: ['Bolas', 'Redes'] }
    ];
    for (const sala of salas) {
        await prisma.sala.upsert({
            where: { nome: sala.nome },
            update: {},
            create: sala
        });
    }
    console.log('✅ Salas criadas');
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
//# sourceMappingURL=seed.js.map