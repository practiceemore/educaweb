"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const bcrypt = require('bcryptjs');
const prisma = new client_1.PrismaClient();
async function main() {
    console.log('🌱 Iniciando seed do banco de dados...');
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
    const disciplinas = [
        { nome: 'Matemática', cargaHoraria: 5, descricao: 'Matemática básica e avançada' },
        { nome: 'Português', cargaHoraria: 4, descricao: 'Língua portuguesa e literatura' },
        { nome: 'História', cargaHoraria: 3, descricao: 'História do Brasil e mundial' },
        { nome: 'Geografia', cargaHoraria: 3, descricao: 'Geografia física e humana' },
        { nome: 'Ciências', cargaHoraria: 3, descricao: 'Ciências naturais' },
        { nome: 'Educação Física', cargaHoraria: 2, descricao: 'Atividades físicas e esportes' },
        { nome: 'Artes', cargaHoraria: 2, descricao: 'Artes visuais e música' },
        { nome: 'Inglês', cargaHoraria: 3, descricao: 'Língua inglesa' }
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
            email: 'joao.silva@escola.com',
            telefone: '(11) 99999-1111',
            especialidade: 'Matemática',
            aulasContratadas: 20,
            salario: 3500.00,
            dataAdmissao: new Date('2023-01-15')
        },
        {
            nome: 'Maria Santos',
            email: 'maria.santos@escola.com',
            telefone: '(11) 99999-2222',
            especialidade: 'Português',
            aulasContratadas: 18,
            salario: 3200.00,
            dataAdmissao: new Date('2023-02-01')
        },
        {
            nome: 'Pedro Oliveira',
            email: 'pedro.oliveira@escola.com',
            telefone: '(11) 99999-3333',
            especialidade: 'História',
            aulasContratadas: 15,
            salario: 3000.00,
            dataAdmissao: new Date('2023-03-10')
        },
        {
            nome: 'Ana Costa',
            email: 'ana.costa@escola.com',
            telefone: '(11) 99999-4444',
            especialidade: 'Geografia',
            aulasContratadas: 15,
            salario: 3000.00,
            dataAdmissao: new Date('2023-03-15')
        },
        {
            nome: 'Carlos Lima',
            email: 'carlos.lima@escola.com',
            telefone: '(11) 99999-5555',
            especialidade: 'Ciências',
            aulasContratadas: 12,
            salario: 2800.00,
            dataAdmissao: new Date('2023-04-01')
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
        {
            nome: '6º Ano A',
            serie: '6º Ano',
            turno: 'manha',
            capacidade: 30,
            alunosMatriculados: 28,
            anoLetivo: '2024'
        },
        {
            nome: '6º Ano B',
            serie: '6º Ano',
            turno: 'tarde',
            capacidade: 30,
            alunosMatriculados: 25,
            anoLetivo: '2024'
        },
        {
            nome: '7º Ano A',
            serie: '7º Ano',
            turno: 'manha',
            capacidade: 30,
            alunosMatriculados: 30,
            anoLetivo: '2024'
        },
        {
            nome: '7º Ano B',
            serie: '7º Ano',
            turno: 'tarde',
            capacidade: 30,
            alunosMatriculados: 27,
            anoLetivo: '2024'
        },
        {
            nome: '8º Ano A',
            serie: '8º Ano',
            turno: 'manha',
            capacidade: 30,
            alunosMatriculados: 29,
            anoLetivo: '2024'
        },
        {
            nome: '9º Ano A',
            serie: '9º Ano',
            turno: 'manha',
            capacidade: 30,
            alunosMatriculados: 26,
            anoLetivo: '2024'
        }
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
        {
            nome: 'Sala 101',
            capacidade: 30,
            tipo: 'normal',
            recursos: 'Quadro branco, projetor',
            status: 'disponivel'
        },
        {
            nome: 'Sala 102',
            capacidade: 30,
            tipo: 'normal',
            recursos: 'Quadro branco, projetor',
            status: 'disponivel'
        },
        {
            nome: 'Sala 103',
            capacidade: 30,
            tipo: 'normal',
            recursos: 'Quadro branco, projetor',
            status: 'disponivel'
        },
        {
            nome: 'Laboratório de Ciências',
            capacidade: 25,
            tipo: 'laboratorio',
            recursos: 'Mesa de laboratório, microscópios, reagentes',
            status: 'disponivel'
        },
        {
            nome: 'Laboratório de Informática',
            capacidade: 20,
            tipo: 'informatica',
            recursos: 'Computadores, projetor, internet',
            status: 'disponivel'
        },
        {
            nome: 'Quadra de Esportes',
            capacidade: 40,
            tipo: 'esportes',
            recursos: 'Quadra coberta, equipamentos esportivos',
            status: 'disponivel'
        }
    ];
    for (const sala of salas) {
        await prisma.sala.upsert({
            where: { nome: sala.nome },
            update: {},
            create: sala
        });
    }
    console.log('✅ Salas criadas');
    const configuracaoGrade = await prisma.configuracaoGrade.upsert({
        where: { nome: 'Grade Padrão 2024' },
        update: {},
        create: {
            nome: 'Grade Padrão 2024',
            horariosInicio: JSON.stringify(['07:00', '08:00', '09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00', '19:00', '20:00', '21:00']),
            horariosFim: JSON.stringify(['07:50', '08:50', '09:50', '10:50', '11:50', '13:50', '14:50', '15:50', '16:50', '17:50', '19:50', '20:50', '21:50']),
            diasSemana: JSON.stringify(['segunda', 'terca', 'quarta', 'quinta', 'sexta']),
            configuracoes: JSON.stringify({
                intervalo: '10:50-11:00',
                almoco: '11:50-13:00',
                jantar: '17:50-19:00'
            }),
            ativa: true
        }
    });
    console.log('✅ Configuração de grade criada');
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
//# sourceMappingURL=seed.sqlite.js.map