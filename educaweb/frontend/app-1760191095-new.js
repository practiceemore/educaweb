// ===== EDUCACWEB - SISTEMA DE GESTÃO EDUCACIONAL =====

// Configuração
const API_BASE_URL = 'http://localhost:3001/api';
let authToken = localStorage.getItem('authToken');
let currentUser = null;

// Arrays de dados
let disciplinas = [];
let professores = [];
let turmas = [];
let salas = [];

// ===== FUNÇÕES DE UTILIDADE =====

function showNotification(message, type = 'info') {
    // Fecha qualquer modal aberto para não ocultar o toast
    const openModals = document.querySelectorAll('.modal');
    openModals.forEach(m => {
        // Não fecha automaticamente em sucesso para manter fluxo
        if (type !== 'success') m.style.display = 'none';
    });

    const containerId = 'toast-container';
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.style.position = 'fixed';
        container.style.top = '50%';
        container.style.left = '50%';
        container.style.transform = 'translate(-50%, -50%)';
        container.style.zIndex = '99999';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '8px';
        document.body.appendChild(container);
    }

    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.minWidth = '260px';
    notification.style.maxWidth = '420px';
    notification.style.padding = '12px 14px';
    notification.style.borderRadius = '6px';
    notification.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
    notification.style.background = type === 'success' ? '#e6ffed' : type === 'error' ? '#ffecec' : '#f1f5f9';
    notification.style.color = '#222';
    notification.style.border = `1px solid ${type === 'success' ? '#b7f5c5' : type === 'error' ? '#ffb3b3' : '#d7e0ea'}`;

    container.appendChild(notification);

    setTimeout(() => {
        notification.remove();
        if (!container.children.length) container.remove();
    }, 3500);
}

function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'block';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
    }
}

// ===== FUNÇÕES DE API =====

async function apiRequest(endpoint, method = 'GET', data = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    // Atualizar token a cada requisição
    const currentToken = localStorage.getItem("authToken");
    
    const config = {
        method: method,
        headers: {
            "Content-Type": "application/json",
            ...(currentToken && { "Authorization": `Bearer ${currentToken}` })
        },
        ...(data && { body: JSON.stringify(data) })
    };

    console.log("Request config:", config);

    try {
        const response = await fetch(url, config);
        const data = await response.json();

        console.log("Response status:", response.status);
        console.log("Response data:", data);

        if (!response.ok) {
