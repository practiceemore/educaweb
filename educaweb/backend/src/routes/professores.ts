import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { CreateProfessorRequest, UpdateProfessorRequest } from '../types';

const router = Router();

// Listar todos os professores
router.get('/', async (req: Request, res: Response) => {
  try {
    const { page = 1, limit = 10, search, ativo } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const where: any = {};
    
    if (search) {
      where.OR = [
        { nome: { contains: search as string, mode: 'insensitive' as const } },
        { email: { contains: search as string, mode: 'insensitive' as const } },
        { especialidade: { contains: search as string, mode: 'insensitive' as const } }
      ];
    }

    if (ativo !== undefined) {
      where.ativo = ativo === 'true';
    }

    const [professores, total] = await Promise.all([
      prisma.professor.findMany({
        where,
        skip,
        take: Number(limit),
        orderBy: { nome: 'asc' },
        include: {
          disciplinas: {
            include: {
              disciplina: {
                select: {
                  id: true,
                  nome: true,
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
      prisma.professor.count({ where })
    ]);

    res.json({
      success: true,
      data: professores,
      pagination: {
        total,
        page: Number(page),
        limit: Number(limit),
        totalPages: Math.ceil(total / Number(limit))
      }
    });

  } catch (error) {
    console.error('Erro ao listar professores:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter professor por ID
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const professor = await prisma.professor.findUnique({
      where: { id: Number(id) },
      include: {
        disciplinas: {
          include: {
            disciplina: {
              select: {
                id: true,
                nome: true,
              }
            }
          }
        },
        gradeHoraria: {
          include: {
            disciplina: {
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

    if (!professor) {
      return res.status(404).json({
        success: false,
        error: 'Professor não encontrado'
      });
    }

    res.json({
      success: true,
      data: professor
    });

  } catch (error) {
    console.error('Erro ao obter professor:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Criar novo professor
router.post('/', async (req: Request, res: Response) => {
  try {
    const { 
      nome, 
      email, 
      telefone, 
      especialidade, 
      aulasContratadas, 
      salario, 
      dataAdmissao,
      disciplinas = []
    }: CreateProfessorRequest = req.body;

    // Validar dados
    if (!nome || !email || !especialidade || !aulasContratadas) {
      return res.status(400).json({
        success: false,
        error: 'Nome, email, especialidade e aulas contratadas são obrigatórios'
      });
    }

    if (aulasContratadas <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Aulas contratadas deve ser maior que zero'
      });
    }

    const professor = await prisma.professor.create({
      data: {
        nome,
        email,
        telefone,
        especialidade,
        aulasContratadas,
        salario,
        dataAdmissao: dataAdmissao ? new Date(dataAdmissao) : null,
        userId: req.user.id,
        disciplinas: {
          create: disciplinas.map(disciplinaId => ({
            disciplinaId
          }))
        }
      },
      include: {
        disciplinas: {
          include: {
            disciplina: {
              select: {
                id: true,
                nome: true,
              }
            }
          }
        }
      }
    });

    res.status(201).json({
      success: true,
      data: professor,
      message: 'Professor criado com sucesso'
    });

  } catch (error) {
    console.error('Erro ao criar professor:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Atualizar professor
router.put('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { 
      nome, 
      email, 
      telefone, 
      especialidade, 
      aulasContratadas, 
      salario, 
      dataAdmissao,
      ativo,
      disciplinas
    }: UpdateProfessorRequest = req.body;

    // Verificar se professor existe
    const existingProfessor = await prisma.professor.findUnique({
      where: { id: Number(id) }
    });

    if (!existingProfessor) {
      return res.status(404).json({
        success: false,
        error: 'Professor não encontrado'
      });
    }

    // Validar dados
    if (aulasContratadas && aulasContratadas <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Aulas contratadas deve ser maior que zero'
      });
    }

    const updateData: any = {};
    if (nome) updateData.nome = nome;
    if (email) updateData.email = email;
    if (telefone !== undefined) updateData.telefone = telefone;
    if (especialidade) updateData.especialidade = especialidade;
    if (aulasContratadas) updateData.aulasContratadas = aulasContratadas;
    if (salario !== undefined) updateData.salario = salario;
    if (dataAdmissao !== undefined) updateData.dataAdmissao = dataAdmissao ? new Date(dataAdmissao) : null;
    if (ativo !== undefined) updateData.ativo = ativo;

    // Atualizar disciplinas se fornecidas
    if (disciplinas !== undefined) {
      await prisma.professorDisciplina.deleteMany({
        where: { professorId: Number(id) }
      });
    }

    const professor = await prisma.professor.update({
      where: { id: Number(id) },
      data: {
        ...updateData,
        ...(disciplinas !== undefined && {
          disciplinas: {
            create: disciplinas.map(disciplinaId => ({
              disciplinaId
            }))
          }
        })
      },
      include: {
        disciplinas: {
          include: {
            disciplina: {
              select: {
                id: true,
                nome: true,
              }
            }
          }
        }
      }
    });

    res.json({
      success: true,
      data: professor,
      message: 'Professor atualizado com sucesso'
    });

  } catch (error) {
    console.error('Erro ao atualizar professor:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Deletar professor
router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se professor existe
    const existingProfessor = await prisma.professor.findUnique({
      where: { id: Number(id) },
      include: {
        _count: {
          select: {
            gradeHoraria: true
          }
        }
      }
    });

    if (!existingProfessor) {
      return res.status(404).json({
        success: false,
        error: 'Professor não encontrado'
      });
    }

    // Verificar se há grade horária agendada
    if (existingProfessor._count.gradeHoraria > 0) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar professor com grade horária agendada'
      });
    }

    await prisma.professor.delete({
      where: { id: Number(id) }
    });

    res.json({
      success: true,
      message: 'Professor deletado com sucesso'
    });

  } catch (error) {
    console.error('Erro ao deletar professor:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
