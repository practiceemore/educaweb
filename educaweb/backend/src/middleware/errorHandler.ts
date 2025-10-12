import { Request, Response, NextFunction } from 'express';
import { Prisma } from '@prisma/client';

export const errorHandler = (
  error: any,
  req: Request,
  res: Response,
  next: NextFunction
) => {
  console.error('Erro capturado:', error);

  // Erro de validação do Prisma
  if (error instanceof Prisma.PrismaClientValidationError) {
    return res.status(400).json({
      success: false,
      error: 'Dados inválidos fornecidos',
      details: error.message
    });
  }

  // Erro de constraint do Prisma
  if (error instanceof Prisma.PrismaClientKnownRequestError) {
    switch (error.code) {
      case 'P2002':
        return res.status(409).json({
          success: false,
          error: 'Registro já existe',
          details: 'Um registro com esses dados já existe no sistema'
        });
      case 'P2025':
        return res.status(404).json({
          success: false,
          error: 'Registro não encontrado',
          details: 'O registro solicitado não foi encontrado'
        });
      case 'P2003':
        return res.status(400).json({
          success: false,
          error: 'Referência inválida',
          details: 'Referência a um registro que não existe'
        });
      default:
        return res.status(500).json({
          success: false,
          error: 'Erro no banco de dados',
          details: error.message
        });
    }
  }

  // Erro de sintaxe JSON
  if (error instanceof SyntaxError && 'body' in error) {
    return res.status(400).json({
      success: false,
      error: 'JSON inválido',
      details: 'O corpo da requisição contém JSON inválido'
    });
  }

  // Erro de validação personalizado
  if (error.name === 'ValidationError') {
    return res.status(400).json({
      success: false,
      error: 'Erro de validação',
      details: error.message
    });
  }

  // Erro de autorização
  if (error.name === 'UnauthorizedError') {
    return res.status(401).json({
      success: false,
      error: 'Não autorizado',
      details: 'Token de acesso inválido ou expirado'
    });
  }

  // Erro padrão do servidor
  const statusCode = error.statusCode || 500;
  const message = error.message || 'Erro interno do servidor';

  res.status(statusCode).json({
    success: false,
    error: message,
    ...(process.env.NODE_ENV === 'development' && { stack: error.stack })
  });
};

// Middleware para capturar rotas não encontradas
export const notFoundHandler = (req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: 'Rota não encontrada',
    details: `A rota ${req.method} ${req.path} não foi encontrada`
  });
};
