import { authMiddleware } from "../middleware/auth";
import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { CreateDisciplinaRequest, UpdateDisciplinaRequest } from '../types';
import { AuthRequest } from '../types';

const router = Router();

// Listar todas as disciplinas
router.get('/', async (req: Request, res: Response) => {
  try {
    const { page = 1, limit = 10, search } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const where = search ? {
      OR: [
        { nome: { contains: search as string, mode: 'insensitive' as const } },
        { descricao: { contains: search as string, mode: 'insensitive' as const } }
      ]
    } : {};

    const [disciplinas, total] = await Promise.all([
      prisma.disciplina.findMany({
        where,
        skip,
        take: Number(limit),
        orderBy: { nome: 'asc' },
        include: {
          professores: {
            include: {
              professor: {
                select: {
                  id: true,
                  nome: true,
                  especialidade: true
                }
              }
            }
          },
          _count: {
            select: {
              gradeHoraria: true
            }
          }
        }
      }),
      prisma.disciplina.count({ where })
    ]);

    res.json({
      success: true,
      data: disciplinas,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });

  } catch (error) {
    console.error('Erro ao listar disciplinas:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter disciplina por ID
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const disciplina = await prisma.disciplina.findUnique({
      where: { id: Number(id) },
      include: {
        professores: {
          include: {
            professor: {
              select: {
                id: true,
                nome: true,
                especialidade: true,
                email: true
              }
            }
          }
        },
        gradeHoraria: {
          include: {
            professor: {
              select: {
                id: true,
                nome: true
              }
            },
            turma: {
              select: {
                id: true,
                nome: true,
                serie: true
              }
            },
            sala: {
              select: {
                id: true,
                nome: true
              }
            }
          }
        }
      }
    });

    if (!disciplina) {
      return res.status(404).json({
        success: false,
        error: 'Disciplina não encontrada'
      });
    }

    res.json({
      success: true,
      data: disciplina
    });

  } catch (error) {
    console.error('Erro ao obter disciplina:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Criar nova disciplina
router.post('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { nome, descricao }: CreateDisciplinaRequest = req.body;

    // Validar dados
    if (!nome) {
      return res.status(400).json({
        success: false,
        error: 'Nome é obrigatório'
      });
    }

    const disciplina = await prisma.disciplina.create({
      data: {
        nome,
        descricao,
        userId: (req as any).user.id,
      },
      include: {
        professores: {
          include: {
            professor: {
              select: {
                id: true,
                nome: true,
                especialidade: true
              }
            }
          }
        }
      }
    });

    res.status(201).json({
      success: true,
      data: disciplina,
      message: 'Disciplina criada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao criar disciplina:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Atualizar disciplina
router.put('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { nome, descricao }: UpdateDisciplinaRequest = req.body;

    // Verificar se disciplina existe
    const existingDisciplina = await prisma.disciplina.findUnique({
      where: { id: Number(id) }
    });

    if (!existingDisciplina) {
      return res.status(404).json({
        success: false,
        error: 'Disciplina não encontrada'
      });
    }

    const updateData: any = {};
    if (nome) updateData.nome = nome;
    if (descricao !== undefined) updateData.descricao = descricao;

    const disciplina = await prisma.disciplina.update({
      where: { id: Number(id) },
      data: updateData,
      include: {
        professores: {
          include: {
            professor: {
              select: {
                id: true,
                nome: true,
                especialidade: true
              }
            }
          }
        }
      }
    });

    res.json({
      success: true,
      data: disciplina,
      message: 'Disciplina atualizada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao atualizar disciplina:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Deletar disciplina
router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se disciplina existe
    const existingDisciplina = await prisma.disciplina.findUnique({
      where: { id: Number(id) },
      include: {
        _count: {
          select: {
            gradeHoraria: true,
            professores: true
          }
        }
      }
    });

    if (!existingDisciplina) {
      return res.status(404).json({
        success: false,
        error: 'Disciplina não encontrada'
      });
    }

    // Verificar se há grade horária ou professores associados
    if (existingDisciplina._count.gradeHoraria > 0) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar disciplina com grade horária agendada'
      });
    }

    if (existingDisciplina._count.professores > 0) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar disciplina com professores associados'
      });
    }

    await prisma.disciplina.delete({
      where: { id: Number(id) }
    });

    res.json({
      success: true,
      message: 'Disciplina deletada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao deletar disciplina:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Estatísticas das disciplinas
router.get('/stats/overview', async (req: Request, res: Response) => {
  try {
    const [
      totalDisciplinas,
      disciplinasComAulas,
      disciplinasSemAulas
    ] = await Promise.all([
      prisma.disciplina.count(),
      prisma.disciplina.count({
        where: {
          gradeHoraria: {
            some: {}
          }
        }
      }),
      prisma.disciplina.count({
        where: {
          gradeHoraria: {
            none: {}
          }
        }
      })
    ]);

    res.json({
      success: true,
      data: {
        totalDisciplinas,
        disciplinasComAulas,
        disciplinasSemAulas
      }
    });

  } catch (error) {
    console.error('Erro ao obter estatísticas:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
