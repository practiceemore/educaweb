import { Router, Request, Response } from 'express';
import { prisma } from '../index';

const router = Router();

// Endpoint para aplicar schema
router.post('/apply-schema', async (req: Request, res: Response): Promise<void> => {
  try {
    console.log('🔗 Aplicando schema...');
    
    // Criar tabela users se não existir
    await prisma.$executeRaw`
      CREATE TABLE IF NOT EXISTS "users" (
        "id" SERIAL PRIMARY KEY,
        "email" VARCHAR(255) UNIQUE NOT NULL,
        "password" VARCHAR(255) NOT NULL,
        "name" VARCHAR(255) NOT NULL,
        "role" VARCHAR(255) DEFAULT 'admin',
        "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `;
    
    console.log('✅ Schema aplicado!');
    
    res.json({
      success: true,
      message: 'Schema aplicado com sucesso'
    });
    
  } catch (error) {
    console.error('❌ Erro ao aplicar schema:', error);
    res.status(500).json({
      success: false,
      error: 'Erro ao aplicar schema'
    });
  }
});

export default router;
