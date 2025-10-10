#!/bin/bash

echo "🎓 EducaWeb - Versão Funcional"
echo "==============================="

# Parar processos existentes
pkill -f "npm run dev" 2>/dev/null
pkill -f "node server.js" 2>/dev/null

echo "🔧 Iniciando backend simples..."

# Backend em background
cd backend
node server.js > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..

echo "⏳ Aguardando backend iniciar..."
sleep 3

echo "🔧 Iniciando frontend..."

# Frontend em background  
cd frontend
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ EducaWeb iniciado com sucesso!"
echo ""
echo "🌐 Frontend: http://localhost:5173"
echo "🔗 Backend: http://localhost:3001"
echo "📊 Health: http://localhost:3001/health"
echo ""
echo "👤 Login: admin@educaweb.com / admin123"
echo ""
echo "📝 Para parar: Ctrl+C"
echo "📋 Logs: tail -f backend.log frontend.log"
echo ""

# Aguardar Ctrl+C
trap "echo '🛑 Parando serviços...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" SIGINT

wait
