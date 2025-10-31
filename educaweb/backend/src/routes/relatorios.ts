import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { AuthRequest } from '../types';

const router = Router();

// Obter todos os dados da escola (mesmo formato do chat/school-context)
router.get('/school-data', async (req: AuthRequest, res: Response) => {
  try {
    const userId = req.user!.id;

    // Buscar todos os dados da escola
    const [turmas, salas, professores, disciplinas, turmaDisciplinas] = await Promise.all([
      // Turmas
      prisma.turma.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          serie: true,
          turno: true,
          capacidade: true,
          alunosMatriculados: true,
          anoLetivo: true,
        },
        orderBy: { nome: 'asc' }
      }),

      // Salas
      prisma.sala.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          capacidade: true,
          tipo: true,
          recursos: true,
          status: true
        },
        orderBy: { nome: 'asc' }
      }),

      // Professores
      prisma.professor.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          email: true,
          telefone: true,
          especialidade: true,
          aulasContratadas: true
        },
        orderBy: { nome: 'asc' }
      }),

      // Disciplinas
      prisma.disciplina.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          descricao: true,
        },
        orderBy: { nome: 'asc' }
      }),

      // Turma-Disciplinas (carga horária)
      prisma.turmaDisciplina.findMany({
        where: { userId },
        include: {
          turma: {
            select: {
              id: true,
              nome: true,
              serie: true
            }
          },
          disciplina: {
            select: {
              id: true,
              nome: true
            }
          }
        },
        orderBy: [
          { turma: { nome: 'asc' } },
          { disciplina: { nome: 'asc' } }
        ]
      })
    ]);

    // Buscar grades horárias para análise de ocupação
    const gradesHoraria = await prisma.gradeHoraria.findMany({
      where: { userId },
      select: {
        id: true,
        turmaId: true,
        status: true
      }
    });

    // Organizar dados da escola
    const schoolData = {
      turmas: turmas.map(t => ({
        ...t,
        disciplinas: turmaDisciplinas
          .filter(td => td.turmaId === t.id)
          .map(td => ({
            nome: td.disciplina.nome,
            aulasPorSemana: td.aulasPorSemana
          }))
      })),
      salas,
      professores,
      disciplinas,
      turmaDisciplinas: turmaDisciplinas.map(td => ({
        turmaId: td.turmaId,
        turmaNome: td.turma.nome,
        disciplinaId: td.disciplinaId,
        disciplinaNome: td.disciplina.nome,
        aulasPorSemana: td.aulasPorSemana
      })),
      estatisticas: {
        totalTurmas: turmas.length,
        totalSalas: salas.length,
        totalProfessores: professores.length,
        totalDisciplinas: disciplinas.length,
        turmasAtivas: turmas.length,
        salasDisponiveis: salas.filter(s => s.status === 'disponivel').length,
        salasOcupadas: salas.filter(s => s.status === 'ocupada').length,
        salasManutencao: salas.filter(s => s.status === 'manutencao').length
      }
    };

    res.json({
      success: true,
      data: schoolData
    });
  } catch (error) {
    console.error('Erro ao buscar dados da escola:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter análises calculadas
router.get('/analytics', async (req: AuthRequest, res: Response) => {
  try {
    const userId = req.user!.id;

    // Buscar dados necessários
    const [turmas, salas, professores, disciplinas, turmaDisciplinas, gradeHoraria] = await Promise.all([
      prisma.turma.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          serie: true,
          turno: true,
          capacidade: true,
          alunosMatriculados: true,
          anoLetivo: true,
        }
      }),
      prisma.sala.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          capacidade: true,
          tipo: true,
          recursos: true,
          status: true
        }
      }),
      prisma.professor.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          especialidade: true,
          aulasContratadas: true
        }
      }),
      prisma.disciplina.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true
        }
      }),
      prisma.turmaDisciplina.findMany({
        where: { userId },
        include: {
          turma: {
            select: {
              id: true,
              nome: true
            }
          },
          disciplina: {
            select: {
              id: true,
              nome: true
            }
          }
        }
      }),
      prisma.gradeHoraria.findMany({
        where: { userId },
        select: {
          id: true,
          turmaId: true,
          status: true
        }
      })
    ]);

    // Buscar professores por disciplina
    const professorDisciplinas = await prisma.professorDisciplina.findMany({
      where: {
        professor: { userId }
      },
      include: {
        professor: {
          select: {
            id: true,
            nome: true
          }
        },
        disciplina: {
          select: {
            id: true,
            nome: true
          }
        }
      }
    });

    // Análise de Carga Horária por Turma
    const cargaHorariaPorTurma = turmas.map(turma => {
      const disciplinasTurma = turmaDisciplinas.filter(td => td.turmaId === turma.id);
      const totalAulasSemana = disciplinasTurma.reduce((sum, td) => sum + td.aulasPorSemana, 0);
      
      return {
        turmaId: turma.id,
        turmaNome: turma.nome,
        serie: turma.serie,
        turno: turma.turno,
        totalAulasSemana,
        disciplinas: disciplinasTurma.map(td => ({
          disciplinaNome: td.disciplina.nome,
          aulasPorSemana: td.aulasPorSemana
        }))
      };
    });

    // Análise de Carga Horária por Disciplina
    const cargaHorariaPorDisciplina = disciplinas.map(disciplina => {
      const turmasComDisciplina = turmaDisciplinas.filter(td => td.disciplinaId === disciplina.id);
      const totalAulasSemana = turmasComDisciplina.reduce((sum, td) => sum + td.aulasPorSemana, 0);
      
      return {
        disciplinaId: disciplina.id,
        disciplinaNome: disciplina.nome,
        totalTurmas: turmasComDisciplina.length,
        totalAulasSemana,
        turmas: turmasComDisciplina.map(td => ({
          turmaNome: td.turma.nome,
          aulasPorSemana: td.aulasPorSemana
        }))
      };
    });

    // Análise de Professores (aulas contratadas vs atribuídas)
    const distribuicaoProfessores = professores.map(professor => {
      const disciplinasProfessor = professorDisciplinas.filter(pd => pd.professorId === professor.id);
      const turmasProfessor = turmaDisciplinas.filter(td => 
        disciplinasProfessor.some(pd => pd.disciplinaId === td.disciplinaId)
      );
      
      // Calcular aulas atribuídas (baseado em turmas com suas disciplinas)
      const aulasAtribuidas = turmasProfessor.reduce((sum, td) => {
        const aulasDisciplina = disciplinasProfessor.find(pd => pd.disciplinaId === td.disciplinaId);
        return aulasDisciplina ? sum + td.aulasPorSemana : sum;
      }, 0);

      return {
        professorId: professor.id,
        professorNome: professor.nome,
        especialidade: professor.especialidade,
        aulasContratadas: professor.aulasContratadas || 0,
        aulasAtribuidas,
        diferenca: (professor.aulasContratadas || 0) - aulasAtribuidas,
        disciplinas: disciplinasProfessor.map(pd => pd.disciplina.nome),
        totalDisciplinas: disciplinasProfessor.length
      };
    });

    // Análise de Ocupação de Salas
    const ocupacaoSalas = salas.map(sala => {
      const capacidade = sala.capacidade || 0;
      const ocupacaoPorTipo = salas.filter(s => s.tipo === sala.tipo);
      
      return {
        salaId: sala.id,
        salaNome: sala.nome,
        tipo: sala.tipo,
        capacidade,
        status: sala.status,
        recursos: sala.recursos,
        disponibilidade: sala.status === 'disponivel' ? 'Disponível' : 
                        sala.status === 'ocupada' ? 'Ocupada' : 'Manutenção'
      };
    });

    // Estatísticas de distribuição por tipo de sala
    const distribuicaoSalasPorTipo = salas.reduce((acc: any, sala) => {
      const tipo = sala.tipo || 'Não especificado';
      if (!acc[tipo]) {
        acc[tipo] = {
          tipo,
          total: 0,
          disponiveis: 0,
          ocupadas: 0,
          manutencao: 0
        };
      }
      acc[tipo].total++;
      if (sala.status === 'disponivel') acc[tipo].disponiveis++;
      else if (sala.status === 'ocupada') acc[tipo].ocupadas++;
      else if (sala.status === 'manutencao') acc[tipo].manutencao++;
      return acc;
    }, {});

    res.json({
      success: true,
      data: {
        cargaHorariaPorTurma,
        cargaHorariaPorDisciplina,
        distribuicaoProfessores,
        ocupacaoSalas,
        distribuicaoSalasPorTipo: Object.values(distribuicaoSalasPorTipo),
        resumo: {
          totalTurmas: turmas.length,
          totalSalas: salas.length,
          totalProfessores: professores.length,
          totalDisciplinas: disciplinas.length,
          totalAulasSemana: cargaHorariaPorTurma.reduce((sum, t) => sum + t.totalAulasSemana, 0),
          salasDisponiveis: salas.filter(s => s.status === 'disponivel').length,
          professoresComDisciplina: professores.filter(p => 
            professorDisciplinas.some(pd => pd.professorId === p.id)
          ).length
        }
      }
    });
  } catch (error) {
    console.error('Erro ao calcular análises:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;

