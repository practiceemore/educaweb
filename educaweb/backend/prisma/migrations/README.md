# Migration: Tornar campos de Turma opcionais

## Sobre

Esta migration torna os campos `serie`, `capacidade` e `anoLetivo` opcionais na tabela `turmas`, deixando apenas `nome` e `turno` como obrigatórios.

## Como aplicar

### Opção 1: Usando Prisma DB Push (Recomendado)

No Render, após fazer deploy, o `postinstall` script já executa `prisma generate`. 

Para aplicar as mudanças do schema:
1. No Render Dashboard, vá para o serviço do backend
2. Abra o Shell/Console
3. Execute:
```bash
cd backend && npx prisma db push
```

Isso sincronizará o schema do Prisma com o banco de dados.

### Opção 2: Executar SQL manualmente

Se preferir executar a migration SQL diretamente:
1. No Render Dashboard, vá para o banco de dados `educaweb-db`
2. Abra o Console/SQL Editor
3. Execute o conteúdo do arquivo `20241227_make_turma_fields_optional.sql`

### Opção 3: Via script local (se tiver acesso ao DATABASE_URL)

```bash
cd educaweb/backend
npx prisma db push
```

## Arquivos

- `20241227_make_turma_fields_optional.sql` - Migration SQL para aplicar manualmente

