import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { authMiddleware, AuthRequest } from '../middleware/auth';
import { 
  CreateGradeHorariaRequest, 
  UpdateGradeHorariaRequest,
  GradeGenerationRequest,
  GradeGenerationResponse
} from '../types';

const router = Router();

// Listar grade horária de uma turma
router.get('/turma/:turmaId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { turmaId } = req.params;

    const gradeHoraria = await prisma.gradeHoraria.findMany({
      where: {
        turmaId: Number(turmaId),
        userId: req.user.id,
        ativa: true
      },
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
        },
        professor: {
          select: {
            id: true,
            nome: true,
            especialidade: true
          }
        },
        sala: {
          select: {
            id: true,
            nome: true,
            tipo: true
          }
        }
      },
      orderBy: [
        { diaSemana: 'asc' },
        { horarioInicio: 'asc' }
      ]
    });

    res.json({
      success: true,
      data: gradeHoraria
    });
  } catch (error) {
    console.error('Erro ao listar grade horária:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Listar todas as grades horárias
router.get('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { page = 1, limit = 100 } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const [gradeHoraria, total] = await Promise.all([
      prisma.gradeHoraria.findMany({
        where: {
          userId: req.user.id,
          ativa: true
        },
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
          },
          professor: {
            select: {
              id: true,
              nome: true,
              especialidade: true
            }
          },
          sala: {
            select: {
              id: true,
              nome: true,
              tipo: true
            }
          }
        },
        orderBy: [
          { turma: { nome: 'asc' } },
          { diaSemana: 'asc' },
          { horarioInicio: 'asc' }
        ],
        skip,
        take: Number(limit)
      }),
      prisma.gradeHoraria.count({
        where: {
          userId: req.user.id,
          ativa: true
        }
      })
    ]);

    res.json({
      success: true,
      data: gradeHoraria,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });
  } catch (error) {
    console.error('Erro ao listar grades horárias:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Adicionar aula à grade horária
router.post('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { 
      turmaId, 
      disciplinaId, 
      professorId, 
      salaId, 
      diaSemana, 
      horarioInicio, 
      horarioFim 
    }: CreateGradeHorariaRequest = req.body;

    // Validar dados
    if (!turmaId || !disciplinaId || !professorId || !salaId || !diaSemana || !horarioInicio || !horarioFim) {
      return res.status(400).json({
        success: false,
        error: 'Todos os campos são obrigatórios'
      });
    }

    // Verificar se já existe aula no mesmo horário para a turma
    const existingAula = await prisma.gradeHoraria.findFirst({
      where: {
        turmaId,
        diaSemana,
        horarioInicio,
        userId: req.user.id,
        ativa: true
      }
    });

    if (existingAula) {
      return res.status(409).json({
        success: false,
        error: 'Já existe uma aula neste horário para esta turma'
      });
    }

    // Verificar conflito de professor
    const professorConflict = await prisma.gradeHoraria.findFirst({
      where: {
        professorId,
        diaSemana,
        horarioInicio,
        userId: req.user.id,
        ativa: true
      }
    });

    if (professorConflict) {
      return res.status(409).json({
        success: false,
        error: 'Professor já está ocupado neste horário'
      });
    }

    // Verificar conflito de sala
    const salaConflict = await prisma.gradeHoraria.findFirst({
      where: {
        salaId,
        diaSemana,
        horarioInicio,
        userId: req.user.id,
        ativa: true
      }
    });

    if (salaConflict) {
      return res.status(409).json({
        success: false,
        error: 'Sala já está ocupada neste horário'
      });
    }

    const gradeHoraria = await prisma.gradeHoraria.create({
      data: {
        turmaId,
        disciplinaId,
        professorId,
        salaId,
        diaSemana,
        horarioInicio,
        horarioFim,
        userId: req.user.id
      },
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
        },
        professor: {
          select: {
            id: true,
            nome: true,
            especialidade: true
          }
        },
        sala: {
          select: {
            id: true,
            nome: true,
            tipo: true
          }
        }
      }
    });

    res.status(201).json({
      success: true,
      data: gradeHoraria,
      message: 'Aula adicionada à grade horária com sucesso'
    });
  } catch (error) {
    console.error('Erro ao adicionar aula à grade:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Atualizar aula na grade horária
router.put('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { 
      disciplinaId, 
      professorId, 
      salaId, 
      diaSemana, 
      horarioInicio, 
      horarioFim, 
      ativa 
    }: UpdateGradeHorariaRequest = req.body;

    // Verificar se a aula existe
    const existingAula = await prisma.gradeHoraria.findFirst({
      where: {
        id: Number(id),
        userId: req.user.id
      }
    });

    if (!existingAula) {
      return res.status(404).json({
        success: false,
        error: 'Aula não encontrada'
      });
    }

    // Se está alterando horário, verificar conflitos
    if (diaSemana || horarioInicio) {
      const checkDia = diaSemana || existingAula.diaSemana;
      const checkHorario = horarioInicio || existingAula.horarioInicio;
      const checkProfessor = professorId || existingAula.professorId;
      const checkSala = salaId || existingAula.salaId;

      // Verificar conflito de professor
      const professorConflict = await prisma.gradeHoraria.findFirst({
        where: {
          professorId: checkProfessor,
          diaSemana: checkDia,
          horarioInicio: checkHorario,
          userId: req.user.id,
          ativa: true,
          id: { not: Number(id) }
        }
      });

      if (professorConflict) {
        return res.status(409).json({
          success: false,
          error: 'Professor já está ocupado neste horário'
        });
      }

      // Verificar conflito de sala
      const salaConflict = await prisma.gradeHoraria.findFirst({
        where: {
          salaId: checkSala,
          diaSemana: checkDia,
          horarioInicio: checkHorario,
          userId: req.user.id,
          ativa: true,
          id: { not: Number(id) }
        }
      });

      if (salaConflict) {
        return res.status(409).json({
          success: false,
          error: 'Sala já está ocupada neste horário'
        });
      }
    }

    const gradeHoraria = await prisma.gradeHoraria.update({
      where: {
        id: Number(id),
        userId: req.user.id
      },
      data: {
        ...(disciplinaId && { disciplinaId }),
        ...(professorId && { professorId }),
        ...(salaId && { salaId }),
        ...(diaSemana && { diaSemana }),
        ...(horarioInicio && { horarioInicio }),
        ...(horarioFim && { horarioFim }),
        ...(ativa !== undefined && { ativa })
      },
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
        },
        professor: {
          select: {
            id: true,
            nome: true,
            especialidade: true
          }
        },
        sala: {
          select: {
            id: true,
            nome: true,
            tipo: true
          }
        }
      }
    });

    res.json({
      success: true,
      data: gradeHoraria,
      message: 'Aula atualizada com sucesso'
    });
  } catch (error) {
    console.error('Erro ao atualizar aula:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Remover aula da grade horária
router.delete('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;

    await prisma.gradeHoraria.delete({
      where: {
        id: Number(id),
        userId: req.user.id
      }
    });

    res.json({
      success: true,
      message: 'Aula removida da grade horária com sucesso'
    });
  } catch (error) {
    console.error('Erro ao remover aula da grade:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Gerar grade horária automaticamente (endpoint para IA)
router.post('/generate', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { turmaId, horariosDisponiveis, diasSemana }: GradeGenerationRequest = req.body;

    // Buscar dados necessários
    const userId = req.user.id;
    
    // Se turmaId não especificado, buscar todas as turmas
    const turmas = turmaId 
      ? await prisma.turma.findMany({ where: { id: turmaId, userId } })
      : await prisma.turma.findMany({ where: { userId, ativa: true } });

    if (turmas.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Nenhuma turma encontrada'
      });
    }

    // Buscar disciplinas das turmas
    const turmaDisciplinas = await prisma.turmaDisciplina.findMany({
      where: {
        turmaId: { in: turmas.map(t => t.id) },
        userId
      },
      include: {
        turma: { select: { id: true, nome: true, serie: true } },
        disciplina: { select: { id: true, nome: true } }
      }
    });

    // Buscar professores disponíveis
    const professores = await prisma.professor.findMany({
      where: { userId, ativo: true },
      select: { id: true, nome: true, especialidade: true }
    });

    // Buscar salas disponíveis
    const salas = await prisma.sala.findMany({
      where: { userId, status: 'disponivel' },
      select: { id: true, nome: true, tipo: true, capacidade: true }
    });

    // Configurações padrão se não fornecidas
    const horarios = horariosDisponiveis || [
      { inicio: '07:00', fim: '08:00' },
      { inicio: '08:00', fim: '09:00' },
      { inicio: '09:00', fim: '10:00' },
      { inicio: '10:00', fim: '11:00' },
      { inicio: '11:00', fim: '12:00' },
      { inicio: '13:00', fim: '14:00' },
      { inicio: '14:00', fim: '15:00' },
      { inicio: '15:00', fim: '16:00' },
      { inicio: '16:00', fim: '17:00' }
    ];

    const dias = diasSemana || ['segunda', 'terca', 'quarta', 'quinta', 'sexta'];

    // Preparar dados para a IA
    const schoolData = {
      turmas: turmas.map(t => ({
        ...t,
        disciplinas: turmaDisciplinas
          .filter(td => td.turmaId === t.id)
          .map(td => ({
            id: td.disciplina.id,
            nome: td.disciplina.nome,
            aulasPorSemana: td.aulasPorSemana
          }))
      })),
      professores,
      salas,
      horarios,
      dias
    };

    res.json({
      success: true,
      data: schoolData,
      message: 'Dados preparados para geração de grade horária'
    });
  } catch (error) {
    console.error('Erro ao preparar geração de grade:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;

