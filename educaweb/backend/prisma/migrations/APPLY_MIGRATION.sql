-- Instruções para aplicar via Render Database Console:
-- 1. Vá para o banco educaweb-db no Render Dashboard
-- 2. Clique em "Connect" ou "PSQL" 
-- 3. Cole e execute este SQL:

BEGIN;

-- Tornar serie opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "serie" DROP NOT NULL;

-- Tornar capacidade opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "capacidade" DROP NOT NULL;

-- Tornar anoLetivo opcional (NULL permitido)
ALTER TABLE turmas ALTER COLUMN "anoLetivo" DROP NOT NULL;

COMMIT;

-- Verificar se funcionou:
SELECT column_name, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'turmas' 
AND column_name IN ('serie', 'capacidade', 'anoLetivo');

