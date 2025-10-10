#!/bin/bash

echo "�� EducaWeb - Sistema de Gestão Educacional"
echo "=============================================="

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não encontrado. Instale o Docker primeiro."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose não encontrado. Instale o Docker Compose primeiro."
    exit 1
fi

echo "🐳 Iniciando serviços com Docker..."

# Parar containers existentes
docker-compose down

# Construir e iniciar containers
docker-compose up --build -d

echo "⏳ Aguardando serviços iniciarem..."
sleep 10

# Verificar se o banco está pronto
echo "🗄️ Configurando banco de dados..."

# Executar migrações
docker-compose exec -T backend npx prisma migrate dev --name init

# Executar seed
docker-compose exec -T backend npm run prisma:seed

echo ""
echo "✅ EducaWeb iniciado com sucesso!"
echo ""
echo "🌐 Frontend: http://localhost:5173"
echo "🔗 Backend API: http://localhost:3001/api"
echo "📊 Prisma Studio: http://localhost:5555"
echo ""
echo "👤 Usuário padrão:"
echo "   Email: admin@educaweb.com"
echo "   Senha: admin123"
echo ""
echo "📝 Para parar os serviços: docker-compose down"
echo "📋 Para ver logs: docker-compose logs -f"
