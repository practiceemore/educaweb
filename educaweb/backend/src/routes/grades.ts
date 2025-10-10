import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { GradeHorariaRequest, GradeHorariaResponse } from '../types';

const router = Router();

// Listar configurações de grade
router.get('/', async (req: Request, res: Response) => {
  try {
    const configuracoes = await prisma.configuracaoGrade.findMany({
      orderBy: { createdAt: 'desc' }
    });

    const formattedConfigs = configuracoes.map(config => ({
      ...config,
      horariosInicio: JSON.parse(config.horariosInicio),
      horariosFim: JSON.parse(config.horariosFim),
      diasSemana: JSON.parse(config.diasSemana),
      configuracoes: config.configuracoes ? JSON.parse(config.configuracoes) : null
    }));

    res.json({
      success: true,
      data: formattedConfigs
    });

  } catch (error) {
    console.error('Erro ao listar configurações:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter configuração ativa
router.get('/active', async (req: Request, res: Response) => {
  try {
    const configuracao = await prisma.configuracaoGrade.findFirst({
      where: { ativa: true }
    });

    if (!configuracao) {
      return res.status(404).json({
        success: false,
        error: 'Nenhuma configuração ativa encontrada'
      });
    }

    const formattedConfig = {
      ...configuracao,
      horariosInicio: JSON.parse(configuracao.horariosInicio),
      horariosFim: JSON.parse(configuracao.horariosFim),
      diasSemana: JSON.parse(configuracao.diasSemana),
      configuracoes: configuracao.configuracoes ? JSON.parse(configuracao.configuracoes) : null
    };

    res.json({
      success: true,
      data: formattedConfig
    });

  } catch (error) {
    console.error('Erro ao obter configuração ativa:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Criar nova configuração
router.post('/', async (req: Request, res: Response) => {
  try {
    const { nome, horariosInicio, horariosFim, diasSemana, configuracoes }: GradeHorariaRequest = req.body;

    if (!nome || !horariosInicio || !horariosFim || !diasSemana) {
      return res.status(400).json({
        success: false,
        error: 'Nome, horários e dias da semana são obrigatórios'
      });
    }

    // Desativar configurações anteriores
    await prisma.configuracaoGrade.updateMany({
      where: { ativa: true },
      data: { ativa: false }
    });

    const configuracao = await prisma.configuracaoGrade.create({
      data: {
        nome,
        horariosInicio: JSON.stringify(horariosInicio),
        horariosFim: JSON.stringify(horariosFim),
        diasSemana: JSON.stringify(diasSemana),
        configuracoes: configuracoes ? JSON.stringify(configuracoes) : null,
        ativa: true,
        user: {
          connect: { id: (req as any).user.id }
        }
      }
    });

    const formattedConfig: GradeHorariaResponse = {
      id: configuracao.id,
      nome: configuracao.nome,
      horariosInicio,
      horariosFim,
      diasSemana,
      configuracoes: configuracoes || {},
      ativa: configuracao.ativa,
      createdAt: configuracao.createdAt.toISOString(),
      updatedAt: configuracao.updatedAt.toISOString()
    };

    res.status(201).json({
      success: true,
      data: formattedConfig,
      message: 'Configuração criada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao criar configuração:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Ativar configuração
router.put('/:id/activate', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se configuração existe
    const configuracao = await prisma.configuracaoGrade.findUnique({
      where: { id: Number(id) }
    });

    if (!configuracao) {
      return res.status(404).json({
        success: false,
        error: 'Configuração não encontrada'
      });
    }

    // Desativar todas as outras
    await prisma.configuracaoGrade.updateMany({
      where: { ativa: true },
      data: { ativa: false }
    });

    // Ativar a selecionada
    await prisma.configuracaoGrade.update({
      where: { id: Number(id) },
      data: { ativa: true }
    });

    res.json({
      success: true,
      message: 'Configuração ativada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao ativar configuração:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Deletar configuração
router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Verificar se configuração existe
    const configuracao = await prisma.configuracaoGrade.findUnique({
      where: { id: Number(id) }
    });

    if (!configuracao) {
      return res.status(404).json({
        success: false,
        error: 'Configuração não encontrada'
      });
    }

    // Não permitir deletar configuração ativa
    if (configuracao.ativa) {
      return res.status(400).json({
        success: false,
        error: 'Não é possível deletar configuração ativa'
      });
    }

    await prisma.configuracaoGrade.delete({
      where: { id: Number(id) }
    });

    res.json({
      success: true,
      message: 'Configuração deletada com sucesso'
    });

  } catch (error) {
    console.error('Erro ao deletar configuração:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
