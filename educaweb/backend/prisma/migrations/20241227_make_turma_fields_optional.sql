-- Migration: Tornar serie, capacidade e anoLetivo opcionais na tabela turmas
-- Data: 2024-12-27

-- Tornar serie opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "serie" DROP NOT NULL;

-- Tornar capacidade opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "capacidade" DROP NOT NULL;

-- Tornar anoLetivo opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "anoLetivo" DROP NOT NULL;

