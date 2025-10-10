import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { CreateSalaRequest, UpdateSalaRequest } from '../types';

const router = Router();

// Listar todas as salas
router.get('/', async (req: Request, res: Response) => {
  try {
    const { page = 1, limit = 10, search, status } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const where: any = {};
    
    if (search) {
      where.OR = [
        { nome: { contains: search as string, mode: 'insensitive' } },
        { tipo: { contains: search as string, mode: 'insensitive' } }
      ];
    }

    if (status) {
      where.status = status;
    }

    const [salas, total] = await Promise.all([
      prisma.sala.findMany({
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
      prisma.sala.count({ where })
    ]);

    res.json({
      success: true,
      data: salas,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });

  } catch (error) {
    console.error('Erro ao listar salas:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter sala por ID
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const sala = await prisma.sala.findUnique({
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
            turma: {
              select: {
                id: true,
                nome: true,
                serie: true
              }
            }
          }
        }
      }
    });

    if (!sala) {
      return res.status(404).json({
        success: false,
        error: 'Sala não encontrada'
      });
    }

    res.json({
      success: true,
      data: sala
    });

  } catch (error) {
    console.error('Erro ao obter sala:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Criar nova sala
router.post('/', async (req: Request, res: Response) => {
  try {
    const { nome, capacidade, tipo, recursos, status = 'disponivel' }: CreateSalaRequest = req.body;

    // Validar dados
    if (!nome || !capacidade || !tipo) {
      return res.status(400).json({
        success: false,
        error: 'Nome, capacidade e tipo são obrigatórios'
      });
    }

    if (capacidade <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Capacidade deve ser maior que zero'
      });
    }

    const sala = await prisma.sala.create({
      data: {
        nome,
        capacidade,
        tipo,
        recursos,
        status,
        userId: (req as any).user.id
      }
    });

    res.status(201).json({
      success: true,
      data: sala,
      message: 'Sala criada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao criar sala:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;

// Deletar sala
router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se a sala existe
    const sala = await prisma.sala.findUnique({
      where: { id: Number(id) },
      include: {
        _count: {
          select: {
            gradeHoraria: true
          }
        }
      }
    });

    if (!sala) {
      return res.status(404).json({
        success: false,
        error: 'Sala não encontrada'
      });
    }

    // Verificar se a sala tem grade horária associada
    if (sala._count.gradeHoraria > 0) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar uma sala que possui grade horária associada'
      });
    }

    // Deletar a sala
    await prisma.sala.delete({
      where: { id: Number(id) }
    });

    res.json({
      success: true,
      message: 'Sala deletada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao deletar sala:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

