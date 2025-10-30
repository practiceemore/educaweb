import { Router, Request, Response } from 'express';
import { authMiddleware } from '../middleware/auth';
import { AuthRequest } from '../types';
import { prisma } from '../index';
import { CreateTurmaRequest, UpdateTurmaRequest } from '../types';

const router = Router();

// Listar todas as turmas
router.get('/', async (req: Request, res: Response) => {
  try {
    const { page = 1, limit = 10, search, ativa } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const where: any = {};
    const userId = (req as any).user?.id;
    
    if (search) {
      where.OR = [
        { nome: { contains: search as string, mode: 'insensitive' as const } },
        { serie: { contains: search as string, mode: 'insensitive' as const } }
      ];
    }

    if (ativa !== undefined) {
      where.ativa = ativa === 'true';
    }

    if (userId) {
      where.userId = userId;
    }

    const [turmas, total] = await Promise.all([
      prisma.turma.findMany({
        where,
        skip,
        take: Number(limit),
        orderBy: { nome: 'asc' },
        include: {
          _count: {
            select: {
              gradeHoraria: true
            }
          }
        }
      }),
      prisma.turma.count({ where })
    ]);

    res.json({
      success: true,
      data: turmas,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });

  } catch (error) {
    console.error('Erro ao listar turmas:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter turma por ID
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const turma = await prisma.turma.findUnique({
      where: { id: Number(id) },
      include: {
        gradeHoraria: {
          include: {
            disciplina: {
              select: {
                id: true,
                nome: true
              }
            },
            professor: {
              select: {
                id: true,
                nome: true
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

    if (!turma) {
      return res.status(404).json({
        success: false,
        error: 'Turma não encontrada'
      });
    }

    res.json({
      success: true,
      data: turma
    });

  } catch (error) {
    console.error('Erro ao obter turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Criar nova turma
router.post('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { nome, serie, turno, capacidade, alunosMatriculados = 0, anoLetivo }: CreateTurmaRequest = req.body;

    // Validar dados
    if (!nome || !serie || !turno || !capacidade || !anoLetivo) {
      return res.status(400).json({
        success: false,
        error: 'Nome, série, turno, capacidade e ano letivo são obrigatórios'
      });
    }

    if (capacidade <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Capacidade deve ser maior que zero'
      });
    }

    if (alunosMatriculados < 0 || alunosMatriculados > capacidade) {
      return res.status(400).json({
        success: false,
        error: 'Alunos matriculados deve estar entre 0 e a capacidade da turma'
      });
    }

    const turma = await prisma.turma.create({
      data: {
        nome,
        serie,
        turno,
        capacidade,
        alunosMatriculados,
        userId: req.user.id,
        anoLetivo
      }
    });

    res.status(201).json({
      success: true,
      data: turma,
      message: 'Turma criada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao criar turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Atualizar turma
router.put('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { nome, serie, turno, capacidade, alunosMatriculados, anoLetivo, ativa }: UpdateTurmaRequest = req.body;

    // Verificar se turma existe
    const existingTurma = await prisma.turma.findUnique({
      where: { id: Number(id) }
    });

    if (!existingTurma) {
      return res.status(404).json({
        success: false,
        error: 'Turma não encontrada'
      });
    }

    // Validar dados
    if (capacidade && capacidade <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Capacidade deve ser maior que zero'
      });
    }

    if (alunosMatriculados !== undefined) {
      const cap = capacidade || existingTurma.capacidade;
      if (alunosMatriculados < 0 || alunosMatriculados > cap) {
        return res.status(400).json({
          success: false,
          error: 'Alunos matriculados deve estar entre 0 e a capacidade da turma'
        });
      }
    }

    const updateData: any = {};
    if (nome) updateData.nome = nome;
    if (serie) updateData.serie = serie;
    if (turno) updateData.turno = turno;
    if (capacidade) updateData.capacidade = capacidade;
    if (alunosMatriculados !== undefined) updateData.alunosMatriculados = alunosMatriculados;
    if (anoLetivo) updateData.anoLetivo = anoLetivo;
    if (ativa !== undefined) updateData.ativa = ativa;

    const turma = await prisma.turma.update({
      where: { id: Number(id) },
      data: updateData
    });

    res.json({
      success: true,
      data: turma,
      message: 'Turma atualizada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao atualizar turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Deletar turma
router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se turma existe
    const existingTurma = await prisma.turma.findUnique({
      where: { id: Number(id) },
      include: {
        _count: {
          select: {
            gradeHoraria: true
          }
        }
      }
    });

    if (!existingTurma) {
      return res.status(404).json({
        success: false,
        error: 'Turma não encontrada'
      });
    }

    // Verificar se há grade horária agendada
    if (existingTurma._count.gradeHoraria > 0) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar turma com grade horária agendada'
      });
    }

    await prisma.turma.delete({
      where: { id: Number(id) }
    });

    res.json({
      success: true,
      message: 'Turma deletada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao deletar turma:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
