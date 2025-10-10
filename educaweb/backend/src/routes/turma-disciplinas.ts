import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { authMiddleware, AuthRequest } from '../middleware/auth';
import { 
  CreateTurmaDisciplinaRequest, 
  UpdateTurmaDisciplinaRequest 
} from '../types';

const router = Router();

// Listar disciplinas de uma turma
router.get('/turma/:turmaId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { turmaId } = req.params;
    const { page = 1, limit = 50 } = req.query;

    const skip = (Number(page) - 1) * Number(limit);

    const [turmaDisciplinas, total] = await Promise.all([
      prisma.turmaDisciplina.findMany({
        where: {
          turmaId: Number(turmaId),
          userId: req.user.id
        },
        include: {
          disciplina: {
            select: {
              id: true,
              nome: true,
              descricao: true
            }
          },
          turma: {
            select: {
              id: true,
              nome: true,
              serie: true
            }
          }
        },
        orderBy: {
          disciplina: {
            nome: 'asc'
          }
        },
        skip,
        take: Number(limit)
      }),
      prisma.turmaDisciplina.count({
        where: {
          turmaId: Number(turmaId),
          userId: req.user.id
        }
      })
    ]);

    res.json({
      success: true,
      data: turmaDisciplinas,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });
  } catch (error) {
    console.error('Erro ao listar disciplinas da turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Listar todas as disciplinas disponíveis (para adicionar à turma)
router.get('/disciplinas-disponiveis/:turmaId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { turmaId } = req.params;

    // Buscar disciplinas que já estão na turma
    const disciplinasNaTurma = await prisma.turmaDisciplina.findMany({
      where: {
        turmaId: Number(turmaId),
        userId: req.user.id
      },
      select: {
        disciplinaId: true
      }
    });

    const disciplinasIds = disciplinasNaTurma.map(td => td.disciplinaId);

    // Buscar disciplinas disponíveis (que não estão na turma)
    const disciplinasDisponiveis = await prisma.disciplina.findMany({
      where: {
        userId: req.user.id,
        id: {
          notIn: disciplinasIds
        }
      },
      select: {
        id: true,
        nome: true,
        descricao: true
      },
      orderBy: {
        nome: 'asc'
      }
    });

    res.json({
      success: true,
      data: disciplinasDisponiveis
    });
  } catch (error) {
    console.error('Erro ao listar disciplinas disponíveis:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Adicionar disciplina à turma
router.post('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { turmaId, disciplinaId, aulasPorSemana }: CreateTurmaDisciplinaRequest = req.body;

    // Validar dados
    if (!turmaId || !disciplinaId || !aulasPorSemana) {
      return res.status(400).json({
        success: false,
        error: 'Turma, disciplina e aulas por semana são obrigatórios'
      });
    }

    if (aulasPorSemana <= 0 || aulasPorSemana > 20) {
      return res.status(400).json({
        success: false,
        error: 'Aulas por semana deve ser entre 1 e 20'
      });
    }

    // Verificar se a combinação já existe
    const existing = await prisma.turmaDisciplina.findFirst({
      where: {
        turmaId,
        disciplinaId,
        userId: req.user.id
      }
    });

    if (existing) {
      return res.status(409).json({
        success: false,
        error: 'Esta disciplina já está configurada para esta turma'
      });
    }

    const turmaDisciplina = await prisma.turmaDisciplina.create({
      data: {
        turmaId,
        disciplinaId,
        aulasPorSemana,
        userId: req.user.id
      },
      include: {
        disciplina: {
          select: {
            id: true,
            nome: true,
            descricao: true
          }
        },
        turma: {
          select: {
            id: true,
            nome: true,
            serie: true
          }
        }
      }
    });

    res.status(201).json({
      success: true,
      data: turmaDisciplina,
      message: 'Disciplina adicionada à turma com sucesso'
    });
  } catch (error) {
    console.error('Erro ao adicionar disciplina à turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Atualizar aulas por semana
router.put('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { aulasPorSemana }: UpdateTurmaDisciplinaRequest = req.body;

    if (!aulasPorSemana || aulasPorSemana <= 0 || aulasPorSemana > 20) {
      return res.status(400).json({
        success: false,
        error: 'Aulas por semana deve ser entre 1 e 20'
      });
    }

    const turmaDisciplina = await prisma.turmaDisciplina.update({
      where: {
        id: Number(id),
        userId: req.user.id
      },
      data: {
        aulasPorSemana
      },
      include: {
        disciplina: {
          select: {
            id: true,
            nome: true,
            descricao: true
          }
        },
        turma: {
          select: {
            id: true,
            nome: true,
            serie: true
          }
        }
      }
    });

    res.json({
      success: true,
      data: turmaDisciplina,
      message: 'Configuração atualizada com sucesso'
    });
  } catch (error) {
    console.error('Erro ao atualizar configuração:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Remover disciplina da turma
router.delete('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;

    await prisma.turmaDisciplina.delete({
      where: {
        id: Number(id),
        userId: req.user.id
      }
    });

    res.json({
      success: true,
      message: 'Disciplina removida da turma com sucesso'
    });
  } catch (error) {
    console.error('Erro ao remover disciplina da turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
