#!/bin/bash

echo "🎓 EducaWeb - Início Simples"
echo "============================="

# Parar processos existentes
pkill -f "npm run dev" 2>/dev/null
pkill -f nodemon 2>/dev/null

echo "🔧 Iniciando backend..."

# Backend em background
cd backend
npm run dev > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..

echo "⏳ Aguardando backend iniciar..."
sleep 5

echo "🔧 Iniciando frontend..."

# Frontend em background  
cd frontend
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ EducaWeb iniciado!"
echo ""
echo "🌐 Frontend: http://localhost:5173"
echo "🔗 Backend: http://localhost:3001"
echo ""
echo "👤 Login: admin@educaweb.com / admin123"
echo ""
echo "📝 Para parar: Ctrl+C"
echo "📋 Logs: tail -f backend.log frontend.log"
echo ""

# Aguardar Ctrl+C
trap "echo '🛑 Parando serviços...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" SIGINT

wait
