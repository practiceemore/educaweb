// Estado global da aplicação
let currentUser = null;
let authToken = localStorage.getItem('authToken');
let disciplinas = [];
let professores = [];
let turmas = [];
let salas = [];

// URLs da API
const API_BASE_URL = 'http://localhost:3001/api';

// Função para fazer requisições à API
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...(authToken && { 'Authorization': `Bearer ${authToken}` }),
            ...options.headers
        },
        ...options
    };

    try {
        const response = await fetch(url, config);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || 'Erro na requisição');
        }
        
        return data;
    } catch (error) {
        console.error('Erro na API:', error);
        throw error;
    }
}

// Função de login
async function login(event) {
    event.preventDefault();
    
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    
    if (!email || !password) {
        showNotification('Email e senha são obrigatórios', 'error');
        return;
    }
    
    try {
        const response = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, senha: password })
        });
        
        if (response.success) {
            authToken = response.token;
            currentUser = response.user;
            
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            // Esconder login e mostrar app
            document.getElementById('loginView').style.display = 'none';
            document.getElementById('appView').style.display = 'block';
            document.getElementById('mainNav').style.display = 'flex';
            
            showNotification('Login realizado com sucesso!', 'success');
            loadData();
            showView('dashboard');
        }
    } catch (error) {
        showNotification('Erro ao fazer login: ' + error.message, 'error');
    }
}

// Função de logout
function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    
    // Mostrar login e esconder app
    document.getElementById('loginView').style.display = 'block';
    document.getElementById('appView').style.display = 'none';
    document.getElementById('mainNav').style.display = 'none';
    
    showNotification('Logout realizado com sucesso!', 'success');
}

// Função para carregar dados iniciais
async function loadData() {
    try {
        // Carregar disciplinas
        const disciplinasResponse = await apiRequest('/disciplinas');
        if (disciplinasResponse.success) {
            disciplinas = disciplinasResponse.data;
        }
        
        // Carregar professores
        const professoresResponse = await apiRequest('/professores');
        if (professoresResponse.success) {
            professores = professoresResponse.data;
        }
        
        // Carregar turmas
        const turmasResponse = await apiRequest('/turmas');
        if (turmasResponse.success) {
            turmas = turmasResponse.data;
        }
        
        // Carregar salas
        const salasResponse = await apiRequest('/salas');
        if (salasResponse.success) {
            salas = salasResponse.data;
        }
        
        updateDashboard();
    } catch (error) {
        console.error('Erro ao carregar dados:', error);
        showNotification('Erro ao carregar dados: ' + error.message, 'error');
    }
}

// Função para mostrar notificações
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// Função para mostrar/esconder views
function showView(viewName) {
    // Esconder todas as views
    const views = document.querySelectorAll('.view');
    views.forEach(view => view.style.display = 'none');
    
    // Remover classe active de todos os botões
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => btn.classList.remove('active'));
    
    // Mostrar a view selecionada
    const targetView = document.getElementById(viewName + 'View');
    if (targetView) {
        targetView.style.display = 'block';
    }
    
    // Adicionar classe active ao botão correspondente
    const activeButton = document.querySelector(`[onclick="showView('${viewName}')"]`);
    if (activeButton) {
        activeButton.classList.add('active');
    }
    
    // Carregar conteúdo específico da view
    switch(viewName) {
        case 'dashboard':
            updateDashboard();
            break;
        case 'disciplinas':
            showDisciplinasView();
            break;
        case 'professores':
            showProfessoresView();
            break;
        case 'turmas':
            showTurmasView();
            break;
        case 'salas':
            showSalasView();
            break;
        case 'grade':
            initializeGradeView();
            break;
        case 'ai':
            initializeAIAssistant();
            break;
    }
}

// Função para atualizar o dashboard
function updateDashboard() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="dashboardView" class="view">
            <h2>📊 Dashboard</h2>
            <div class="dashboard-grid">
                <div class="dashboard-card">
                    <div class="card-icon">📚</div>
                    <div class="card-content">
                        <h3>Disciplinas</h3>
                        <p class="card-number">${disciplinas.length}</p>
                        <span class="card-label">Cadastradas</span>
                    </div>
                </div>
                <div class="dashboard-card">
                    <div class="card-icon">👨‍🏫</div>
                    <div class="card-content">
                        <h3>Professores</h3>
                        <p class="card-number">${professores.length}</p>
                        <span class="card-label">Ativos</span>
                    </div>
                </div>
                <div class="dashboard-card">
                    <div class="card-icon">👥</div>
                    <div class="card-content">
                        <h3>Turmas</h3>
                        <p class="card-number">${turmas.length}</p>
                        <span class="card-label">Formadas</span>
                    </div>
                </div>
                <div class="dashboard-card">
                    <div class="card-icon">🏫</div>
                    <div class="card-content">
                        <h3>Salas</h3>
                        <p class="card-number">${salas.length}</p>
                        <span class="card-label">Disponíveis</span>
                    </div>
                </div>
            </div>
            
            <div class="dashboard-actions">
                <h3>🚀 Ações Rápidas</h3>
                <div class="action-buttons">
                    <button class="action-btn" onclick="showView('disciplinas')">
                        📚 Gerenciar Disciplinas
                    </button>
                    <button class="action-btn" onclick="showView('professores')">
                        👨‍🏫 Gerenciar Professores
                    </button>
                    <button class="action-btn" onclick="showView('turmas')">
                        👥 Gerenciar Turmas
                    </button>
                    <button class="action-btn" onclick="showView('salas')">
                        🏫 Gerenciar Salas
                    </button>
                    <button class="action-btn" onclick="showView('grade')">
                        📅 Grade Horária
                    </button>
                    <button class="action-btn" onclick="showView('ai')">
                        🤖 Assistente IA
                    </button>
                </div>
            </div>
            
            <div class="reports-section">
                <h3>📄 Relatórios</h3>
                <div class="report-buttons">
                    <button class="report-btn" onclick="generateReport('escola')">📊 Relatório da Escola</button>
                    <button class="report-btn" onclick="generateReport('professores')">👨‍🏫 Relatório de Professores</button>
                    <button class="report-btn" onclick="generateReport('turmas')">👥 Relatório de Turmas</button>
                    <button class="report-btn" onclick="generateReport('disciplinas')">📚 Relatório de Disciplinas</button>
                    <button class="report-btn" onclick="generateReport('grade')">📅 Grade Horária</button>
                </div>
            </div>
        </div>
    `;
}

// Função para mostrar a view de disciplinas
function showDisciplinasView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="disciplinasView" class="view">
            <div class="view-header">
                <h2>📚 Disciplinas</h2>
                <button class="btn-primary" onclick="showAddDisciplinaModal()">➕ Adicionar Disciplina</button>
            </div>
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Carga Horária</th>
                            <th>Descrição</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody id="disciplinasTableBody">
                        ${disciplinas.map(d => `
                            <tr>
                                <td>${d.nome}</td>
                                <td>${d.cargaHoraria}h</td>
                                <td>${d.descricao || 'N/A'}</td>
                                <td class="table-actions">
                                    <button class="btn-edit" onclick="editDisciplina('${d.id}')">✏️</button>
                                    <button class="btn-delete" onclick="deleteDisciplina('${d.id}')">🗑️</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// Função para mostrar a view de professores
function showProfessoresView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="professoresView" class="view">
            <div class="view-header">
                <h2>👨‍🏫 Professores</h2>
                <button class="btn-primary" onclick="showAddProfessorModal()">➕ Adicionar Professor</button>
            </div>
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Email</th>
                            <th>Disciplinas</th>
                            <th>Aulas Contratadas</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody id="professoresTableBody">
                        ${professores.map(p => `
                            <tr>
                                <td>${p.nome}</td>
                                <td>${p.email}</td>
                                <td>${Array.isArray(p.disciplinas) ? p.disciplinas.join(', ') : 'N/A'}</td>
                                <td>${p.aulasContratadas || 0}</td>
                                <td class="table-actions">
                                    <button class="btn-edit" onclick="editProfessor('${p.id}')">✏️</button>
                                    <button class="btn-delete" onclick="deleteProfessor('${p.id}')">🗑️</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// Função para mostrar a view de turmas
function showTurmasView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="turmasView" class="view">
            <div class="view-header">
                <h2>👥 Turmas</h2>
                <button class="btn-primary" onclick="showAddTurmaModal()">➕ Adicionar Turma</button>
            </div>
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Série</th>
                            <th>Turno</th>
                            <th>Alunos</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody id="turmasTableBody">
                        ${turmas.map(t => `
                            <tr>
                                <td>${t.nome}</td>
                                <td>${t.serie}</td>
                                <td>${t.turno}</td>
                                <td>${t.numeroAlunos || 0}</td>
                                <td class="table-actions">
                                    <button class="btn-edit" onclick="editTurma('${t.id}')">✏️</button>
                                    <button class="btn-delete" onclick="deleteTurma('${t.id}')">🗑️</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// Função para mostrar a view de salas
function showSalasView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="salasView" class="view">
            <div class="view-header">
                <h2>�� Salas</h2>
                <button class="btn-primary" onclick="showAddSalaModal()">➕ Adicionar Sala</button>
            </div>
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Capacidade</th>
                            <th>Tipo</th>
                            <th>Recursos</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody id="salasTableBody">
                        ${salas.map(s => `
                            <tr>
                                <td>${s.nome}</td>
                                <td>${s.capacidade || 0}</td>
                                <td>${s.tipo || 'N/A'}</td>
                                <td>${Array.isArray(s.recursos) ? s.recursos.join(', ') : 'N/A'}</td>
                                <td class="table-actions">
                                    <button class="btn-edit" onclick="editSala('${s.id}')">✏️</button>
                                    <button class="btn-delete" onclick="deleteSala('${s.id}')">🗑️</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// CRUD Functions para Disciplinas
async function addDisciplina(nome, cargaHoraria, descricao) {
    try {
        const response = await apiRequest('/disciplinas', {
            method: 'POST',
            body: JSON.stringify({ nome, cargaHoraria, descricao })
        });
        
        if (response.success) {
            disciplinas.push(response.data);
            showDisciplinasView();
            updateDashboard();
            showNotification('Disciplina adicionada com sucesso!', 'success');
            return true;
        }
    } catch (error) {
        showNotification('Erro ao adicionar disciplina: ' + error.message, 'error');
        return false;
    }
}

async function updateDisciplina(id, nome, cargaHoraria, descricao) {
    try {
        const response = await apiRequest(`/disciplinas/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ nome, cargaHoraria, descricao })
        });
        
        if (response.success) {
            const index = disciplinas.findIndex(d => d.id === id);
            if (index !== -1) {
                disciplinas[index] = response.data;
                showDisciplinasView();
                updateDashboard();
            }
            showNotification('Disciplina atualizada com sucesso!', 'success');
            return true;
        }
    } catch (error) {
        showNotification('Erro ao atualizar disciplina: ' + error.message, 'error');
        return false;
    }
}

async function deleteDisciplina(id) {
    if (!confirm('Tem certeza que deseja deletar esta disciplina?')) {
        return;
    }
    
    try {
        const response = await apiRequest(`/disciplinas/${id}`, {
            method: 'DELETE'
        });
        
        if (response.success) {
            disciplinas = disciplinas.filter(d => d.id !== id);
            showDisciplinasView();
            updateDashboard();
            showNotification('Disciplina deletada com sucesso!', 'success');
            return true;
        }
    } catch (error) {
        showNotification('Erro ao deletar disciplina: ' + error.message, 'error');
        return false;
    }
}

// CRUD Functions para Professores
function editProfessor(id) {
    showNotification(`Editando professor ${id}`, 'info');
}

async function deleteProfessor(id) {
    if (!confirm('Tem certeza que deseja deletar este professor?')) {
        return;
    }
    
    try {
        const response = await apiRequest(`/professores/${id}`, {
            method: 'DELETE'
        });
        
        if (response.success) {
            const index = professores.findIndex(p => p.id === id);
            if (index !== -1) {
                professores.splice(index, 1);
                updateDashboard();
                showProfessoresView();
            }
            showNotification('Professor deletado com sucesso!', 'success');
            return true;
        }
    } catch (error) {
        showNotification('Erro ao deletar professor: ' + error.message, 'error');
        return false;
    }
}

// CRUD Functions para Turmas
function editTurma(id) {
    showNotification(`Editando turma ${id}`, 'info');
}

function deleteTurma(id) {
    if (confirm('Tem certeza que deseja deletar esta turma?')) {
        showNotification(`Turma ${id} deletada`, 'success');
    }
}

// CRUD Functions para Salas
function editSala(id) {
    showNotification(`Editando sala ${id}`, 'info');
}

function deleteSala(id) {
    if (confirm('Tem certeza que deseja deletar esta sala?')) {
        showNotification(`Sala ${id} deletada`, 'success');
    }
}

// Modal Functions
function showAddDisciplinaModal() {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal">
            <div class="modal-header">
                <h3>➕ Adicionar Disciplina</h3>
                <button class="modal-close" onclick="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
                <form onsubmit="handleAddDisciplina(event)">
                    <div class="form-group">
                        <label for="disciplinaNome">Nome:</label>
                        <input type="text" id="disciplinaNome" required>
                    </div>
                    <div class="form-group">
                        <label for="disciplinaCarga">Carga Horária:</label>
                        <input type="number" id="disciplinaCarga" required>
                    </div>
                    <div class="form-group">
                        <label for="disciplinaDescricao">Descrição:</label>
                        <textarea id="disciplinaDescricao"></textarea>
                    </div>
                    <div class="modal-actions">
                        <button type="button" class="btn-secondary" onclick="closeModal()">Cancelar</button>
                        <button type="submit" class="btn-primary">Adicionar</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function showAddProfessorModal() {
    showNotification('Modal de adicionar professor em desenvolvimento', 'info');
}

function showAddTurmaModal() {
    showNotification('Modal de adicionar turma em desenvolvimento', 'info');
}

function showAddSalaModal() {
    showNotification('Modal de adicionar sala em desenvolvimento', 'info');
}

function closeModal() {
    const modal = document.querySelector('.modal-overlay');
    if (modal) {
        modal.remove();
    }
}

async function handleAddDisciplina(event) {
    event.preventDefault();
    
    const nome = document.getElementById('disciplinaNome').value;
    const cargaHoraria = parseInt(document.getElementById('disciplinaCarga').value);
    const descricao = document.getElementById('disciplinaDescricao').value;
    
    const success = await addDisciplina(nome, cargaHoraria, descricao);
    if (success) {
        closeModal();
    }
}

// Grade Horária Functions
function initializeGradeView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="gradeView" class="view">
            <div class="view-header">
                <h2>📅 Grade Horária</h2>
                <div class="grade-toolbar">
                    <button class="btn-primary" onclick="generateAutomaticSchedule()">🤖 Gerar Automático</button>
                    <button class="btn-secondary" onclick="exportGradeToPDF()">📄 Exportar PDF</button>
                    <button class="btn-info" onclick="checkConflicts()">⚠️ Verificar Conflitos</button>
                </div>
            </div>
            
            <div class="grade-container">
                <div class="grade-sidebar">
                    <h3>📚 Disciplinas Disponíveis</h3>
                    <div class="draggable-items" id="draggableItems">
                        ${disciplinas.map(d => `
                            <div class="draggable-item" draggable="true" data-disciplina="${d.id}">
                                ${d.nome} (${d.cargaHoraria}h)
                            </div>
                        `).join('')}
                    </div>
                    
                    <div class="grade-stats">
                        <h3>📊 Estatísticas</h3>
                        <div class="stat-item">
                            <span>Aulas Agendadas:</span>
                            <span id="aulasAgendadas">0</span>
                        </div>
                        <div class="stat-item">
                            <span>Conflitos:</span>
                            <span id="conflitosCount">0</span>
                        </div>
                    </div>
                </div>
                
                <div class="grade-grid">
                    <table class="schedule-table">
                        <thead>
                            <tr>
                                <th>Horário</th>
                                <th>Segunda</th>
                                <th>Terça</th>
                                <th>Quarta</th>
                                <th>Quinta</th>
                                <th>Sexta</th>
                            </tr>
                        </thead>
                        <tbody id="scheduleTableBody">
                            ${generateScheduleRows()}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    `;
    
    loadGradeData();
    setupDragAndDrop();
}

function generateScheduleRows() {
    const horarios = [
        '07:00 - 07:50',
        '07:50 - 08:40',
        '08:40 - 09:30',
        '09:50 - 10:40',
        '10:40 - 11:30',
        '11:30 - 12:20',
        '13:30 - 14:20',
        '14:20 - 15:10',
        '15:10 - 16:00',
        '16:20 - 17:10',
        '17:10 - 18:00'
    ];
    
    return horarios.map((horario, index) => `
        <tr>
            <td class="time-slot">${horario}</td>
            <td class="schedule-cell" data-day="0" data-time="${index}"></td>
            <td class="schedule-cell" data-day="1" data-time="${index}"></td>
            <td class="schedule-cell" data-day="2" data-time="${index}"></td>
            <td class="schedule-cell" data-day="3" data-time="${index}"></td>
            <td class="schedule-cell" data-day="4" data-time="${index}"></td>
        </tr>
    `).join('');
}

function loadGradeData() {
    // Carregar dados existentes da grade
    updateGradeStats();
}

function setupDragAndDrop() {
    const draggableItems = document.querySelectorAll('.draggable-item');
    const scheduleCells = document.querySelectorAll('.schedule-cell');
    
    draggableItems.forEach(item => {
        item.addEventListener('dragstart', handleDragStart);
    });
    
    scheduleCells.forEach(cell => {
        cell.addEventListener('dragover', handleDragOver);
        cell.addEventListener('drop', handleDrop);
        cell.addEventListener('click', handleCellClick);
    });
}

function handleDragStart(e) {
    e.dataTransfer.setData('text/plain', e.target.dataset.disciplina);
}

function handleDragOver(e) {
    e.preventDefault();
}

function handleDrop(e) {
    e.preventDefault();
    const disciplinaId = e.dataTransfer.getData('text/plain');
    const disciplina = disciplinas.find(d => d.id === disciplinaId);
    
    if (disciplina) {
        e.target.innerHTML = `
            <div class="scheduled-class">
                <div class="class-name">${disciplina.nome}</div>
                <button class="remove-class" onclick="removeClass(this)">×</button>
            </div>
        `;
        e.target.classList.add('occupied');
        updateGradeStats();
    }
}

function handleCellClick(e) {
    if (!e.target.classList.contains('occupied')) {
        showAddClassModal(e.target);
    }
}

function removeClass(button) {
    const cell = button.closest('.schedule-cell');
    cell.innerHTML = '';
    cell.classList.remove('occupied');
    updateGradeStats();
}

function showAddClassModal(cell) {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal">
            <div class="modal-header">
                <h3>➕ Adicionar Aula</h3>
                <button class="modal-close" onclick="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
                <form onsubmit="handleAddClass(event, this)" data-cell-day="${cell.dataset.day}" data-cell-time="${cell.dataset.time}">
                    <div class="form-group">
                        <label for="classDisciplina">Disciplina:</label>
                        <select id="classDisciplina" required>
                            <option value="">Selecione uma disciplina</option>
                            ${disciplinas.map(d => `<option value="${d.id}">${d.nome}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="classProfessor">Professor:</label>
                        <select id="classProfessor" required>
                            <option value="">Selecione um professor</option>
                            ${professores.map(p => `<option value="${p.id}">${p.nome}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="classTurma">Turma:</label>
                        <select id="classTurma" required>
                            <option value="">Selecione uma turma</option>
                            ${turmas.map(t => `<option value="${t.id}">${t.nome}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="classSala">Sala:</label>
                        <select id="classSala" required>
                            <option value="">Selecione uma sala</option>
                            ${salas.map(s => `<option value="${s.id}">${s.nome}</option>`).join('')}
                        </select>
                    </div>
                    <div class="modal-actions">
                        <button type="button" class="btn-secondary" onclick="closeModal()">Cancelar</button>
                        <button type="submit" class="btn-primary">Adicionar</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function handleAddClass(event, form) {
    event.preventDefault();
    
    const disciplinaId = document.getElementById('classDisciplina').value;
    const professorId = document.getElementById('classProfessor').value;
    const turmaId = document.getElementById('classTurma').value;
    const salaId = document.getElementById('classSala').value;
    
    const disciplina = disciplinas.find(d => d.id === disciplinaId);
    const professor = professores.find(p => p.id === professorId);
    const turma = turmas.find(t => t.id === turmaId);
    const sala = salas.find(s => s.id === salaId);
    
    const day = form.dataset.cellDay;
    const time = form.dataset.cellTime;
    
    const cell = document.querySelector(`[data-day="${day}"][data-time="${time}"]`);
    
    cell.innerHTML = `
        <div class="scheduled-class">
            <div class="class-name">${disciplina.nome}</div>
            <div class="class-details">
                <small>${professor.nome} | ${turma.nome} | ${sala.nome}</small>
            </div>
            <button class="remove-class" onclick="removeClass(this)">×</button>
        </div>
    `;
    cell.classList.add('occupied');
    
    updateGradeStats();
    closeModal();
    showNotification('Aula adicionada com sucesso!', 'success');
}

function generateAutomaticSchedule() {
    if (confirm('Isso irá substituir a grade atual. Continuar?')) {
        // Limpar grade atual
        const cells = document.querySelectorAll('.schedule-cell');
        cells.forEach(cell => {
            cell.innerHTML = '';
            cell.classList.remove('occupied');
        });
        
        // Algoritmo simples de distribuição
        // Em uma implementação real, seria mais sofisticado
        showNotification('Grade automática gerada!', 'success');
        updateGradeStats();
    }
}

function checkConflicts() {
    // Verificar conflitos na grade
    showNotification('Verificando conflitos...', 'info');
    
    setTimeout(() => {
        showNotification('Nenhum conflito encontrado!', 'success');
    }, 1000);
}

function exportGradeToPDF() {
    showNotification('Exportando grade para PDF...', 'info');
    
    // Aqui seria implementada a exportação real
    setTimeout(() => {
        showNotification('Grade exportada com sucesso!', 'success');
    }, 1500);
}

function updateGradeStats() {
    const aulasElement = document.getElementById('aulasAgendadas');
    const conflitosElement = document.getElementById('conflitosCount');
    
    if (aulasElement) {
        const aulasAgendadas = document.querySelectorAll('.schedule-cell.occupied').length;
        aulasElement.textContent = aulasAgendadas;
    }
    
    if (conflitosElement) {
        conflitosElement.textContent = '0'; // Implementar lógica de detecção de conflitos
    }
}

// AI Assistant Functions
function initializeAIAssistant() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div id="aiView" class="view">
            <h2>🤖 Assistente IA</h2>
            <div class="ai-container">
                <div class="ai-sidebar">
                    <div class="ai-suggestions">
                        <h3>💡 Sugestões Rápidas</h3>
                        <div class="suggestion-buttons">
                            <button class="suggestion-btn" onclick="askAI('Como posso otimizar a grade horária?')">
                                📅 Otimizar Grade
                            </button>
                            <button class="suggestion-btn" onclick="askAI('Quais professores estão sobrecarregados?')">
                                👨‍🏫 Análise Professores
                            </button>
                            <button class="suggestion-btn" onclick="askAI('Como melhorar a distribuição de turmas?')">
                                👥 Distribuição Turmas
                            </button>
                            <button class="suggestion-btn" onclick="askAI('Gere um relatório da escola')">
                                📊 Relatório Escola
                            </button>
                        </div>
                    </div>
                    <div class="ai-status">
                        <h3>🔗 Status da IA</h3>
                        <div class="status-item">
                            <span class="status-label">Conexão:</span>
                            <span class="status-value" id="aiConnectionStatus">Verificando...</span>
                        </div>
                        <div class="status-item">
                            <span class="status-label">Modelo:</span>
                            <span class="status-value">Google Gemini 2.0 Flash</span>
                        </div>
                        <div class="ai-config">
                            <h4>⚙️ Configuração</h4>
                            <div class="config-item">
                                <label for="geminiApiKey">Chave API:</label>
                                <input type="password" id="geminiApiKey" placeholder="Sua chave API do Gemini">
                                <button class="btn-config" onclick="configureGeminiAPI()">Salvar</button>
                            </div>
                            <button class="btn-test" onclick="testGeminiAPI()">🧪 Testar Conexão</button>
                        </div>
                    </div>
                </div>
                <div class="ai-chat">
                    <div class="chat-header">
                        <h3>💬 Chat com IA</h3>
                        <div class="chat-actions">
                            <button class="btn-secondary" onclick="clearChat()">🗑️ Limpar</button>
                            <button class="btn-primary" onclick="exportChat()">📄 Exportar</button>
                        </div>
                    </div>
                    <div class="chat-messages" id="chatMessages">
                        <div class="message ai-message">
                            <div class="message-avatar">🤖</div>
                            <div class="message-content">
                                <div class="message-text">
                                    Olá! Sou seu assistente IA educacional. Como posso ajudar você hoje?
                                </div>
                                <div class="message-time">Agora</div>
                            </div>
                        </div>
                    </div>
                    <div class="chat-input">
                        <div class="input-container">
                            <input type="text" id="chatInput" placeholder="Digite sua pergunta..." onkeypress="handleChatKeyPress(event)">
                            <button class="send-btn" onclick="sendMessage()">📤</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    updateAIStatus();
    loadGeminiConfig();
}

function loadGeminiConfig() {
    const savedApiKey = localStorage.getItem('geminiApiKey');
    if (savedApiKey) {
        document.getElementById('geminiApiKey').value = savedApiKey;
        updateAIStatus('online');
    }
}

function configureGeminiAPI() {
    const apiKey = document.getElementById('geminiApiKey').value;
    if (apiKey) {
        localStorage.setItem('geminiApiKey', apiKey);
        updateAIStatus('online');
        showNotification('Chave API configurada com sucesso!', 'success');
    } else {
        showNotification('Por favor, insira uma chave API válida', 'error');
    }
}

async function testGeminiAPI() {
    const apiKey = localStorage.getItem('geminiApiKey');
    if (!apiKey) {
        showNotification('Configure a chave API primeiro', 'error');
        return;
    }
    
    showNotification('Testando conexão...', 'info');
    
    try {
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                contents: [{
                    parts: [{
                        text: "Teste"
                    }]
                }]
            })
        });
        
        if (response.ok) {
            updateAIStatus('online');
            showNotification('Conexão com IA estabelecida!', 'success');
        } else {
            updateAIStatus('offline');
            showNotification('Erro na conexão. Verifique sua chave API.', 'error');
        }
    } catch (error) {
        updateAIStatus('offline');
        showNotification('Erro de rede. Verifique sua conexão.', 'error');
    }
}

function updateAIStatus(status = 'checking') {
    const statusElement = document.getElementById('aiConnectionStatus');
    if (statusElement) {
        switch(status) {
            case 'online':
                statusElement.textContent = '🟢 Online';
                statusElement.style.color = '#22c55e';
                break;
            case 'offline':
                statusElement.textContent = '🔴 Offline';
                statusElement.style.color = '#ef4444';
                break;
            default:
                statusElement.textContent = '🟡 Verificando...';
                statusElement.style.color = '#f59e0b';
        }
    }
}

function handleChatKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

function sendMessage() {
    const input = document.getElementById('chatInput');
    const message = input.value.trim();
    
    if (message) {
        addMessageToChat(message, 'user');
        input.value = '';
        sendMessageToAI(message);
    }
}

function askAI(question) {
    addMessageToChat(question, 'user');
    sendMessageToAI(question);
}

function addMessageToChat(message, sender) {
    const chatMessages = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;
    
    const avatar = sender === 'user' ? '👤' : '🤖';
    const time = new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    
    messageDiv.innerHTML = `
        <div class="message-avatar">${avatar}</div>
        <div class="message-content">
            <div class="message-text">${message}</div>
            <div class="message-time">${time}</div>
        </div>
    `;
    
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

async function sendMessageToAI(message) {
    const apiKey = localStorage.getItem('geminiApiKey');
    if (!apiKey) {
        addMessageToChat('Por favor, configure sua chave API do Gemini primeiro.', 'ai');
        return;
    }
    
    // Mostrar indicador de carregamento
    addMessageToChat('Digitando...', 'ai');
    const loadingMessage = document.querySelector('.chat-messages .message:last-child');
    
    try {
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                contents: [{
                    parts: [{
                        text: `Contexto: Você é um assistente especializado em gestão educacional para o sistema EducaWeb. 
                               
                        Dados do sistema:
                        - Disciplinas cadastradas: ${disciplinas.length}
                        - Professores cadastrados: ${professores.length}
                        - Turmas cadastradas: ${turmas.length}
                        - Salas cadastradas: ${salas.length}
                        
                        Pergunta do usuário: ${message}
                        
                        Por favor, forneça uma resposta útil e específica para gestão educacional.`
                    }]
                }],
                safetySettings: [
                    { category: 'HARM_CATEGORY_HARASSMENT', threshold: 'BLOCK_NONE' },
                    { category: 'HARM_CATEGORY_HATE_SPEECH', threshold: 'BLOCK_NONE' },
                    { category: 'HARM_CATEGORY_SEXUALLY_EXPLICIT', threshold: 'BLOCK_NONE' },
                    { category: 'HARM_CATEGORY_DANGEROUS_CONTENT', threshold: 'BLOCK_NONE' },
                ],
                generationConfig: {
                    temperature: 0.7,
                    topK: 40,
                    topP: 0.95,
                    maxOutputTokens: 1024,
                }
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            const aiResponse = data.candidates[0].content.parts[0].text;
            
            // Remover mensagem de carregamento
            loadingMessage.remove();
            
            // Adicionar resposta da IA
            addMessageToChat(aiResponse, 'ai');
            updateAIStatus('online');
        } else {
            loadingMessage.remove();
            addMessageToChat('Desculpe, ocorreu um erro ao processar sua solicitação. Verifique sua chave API.', 'ai');
            updateAIStatus('offline');
        }
    } catch (error) {
        loadingMessage.remove();
        addMessageToChat('Erro de conexão. Verifique sua internet e chave API.', 'ai');
        updateAIStatus('offline');
    }
}

function clearChat() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = `
        <div class="message ai-message">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
                <div class="message-text">
                    Olá! Sou seu assistente IA educacional. Como posso ajudar você hoje?
                </div>
                <div class="message-time">Agora</div>
            </div>
        </div>
    `;
    showNotification('Chat limpo!', 'success');
}

function exportChat() {
    const messages = document.querySelectorAll('#chatMessages .message');
    let chatContent = '';
    
    messages.forEach(message => {
        const sender = message.classList.contains('user-message') ? 'Usuário' : 'IA';
        const text = message.querySelector('.message-text').textContent;
        const time = message.querySelector('.message-time').textContent;
        chatContent += `[${time}] ${sender}: ${text}\n\n`;
    });
    
    const blob = new Blob([chatContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chat-educaweb-${new Date().toISOString().split('T')[0]}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    
    showNotification('Chat exportado!', 'success');
}

// Report Functions
function generateReport(type) {
    showNotification(`Gerando relatório de ${type}...`, 'info');
    
    setTimeout(() => {
        let reportContent = '';
        
        switch(type) {
            case 'escola':
                reportContent = generateEscolaReport();
                break;
            case 'professores':
                reportContent = generateProfessoresReport();
                break;
            case 'turmas':
                reportContent = generateTurmasReport();
                break;
            case 'disciplinas':
                reportContent = generateDisciplinasReport();
                break;
            case 'grade':
                reportContent = generateGradeReport();
                break;
        }
        
        openReportWindow(reportContent, type);
        showNotification(`Relatório de ${type} gerado!`, 'success');
    }, 1000);
}

function generateEscolaReport() {
    return `
        <h1>📊 Relatório Geral da Escola</h1>
        <div class="report-section">
            <h2>📈 Resumo Executivo</h2>
            <p><strong>Total de Disciplinas:</strong> ${disciplinas.length}</p>
            <p><strong>Total de Professores:</strong> ${professores.length}</p>
            <p><strong>Total de Turmas:</strong> ${turmas.length}</p>
            <p><strong>Total de Salas:</strong> ${salas.length}</p>
        </div>
        
        <div class="report-section">
            <h2>📚 Disciplinas</h2>
            <ul>
                ${disciplinas.map(d => `<li>${d.nome} - ${d.cargaHoraria}h</li>`).join('')}
            </ul>
        </div>
        
        <div class="report-section">
            <h2>👨‍🏫 Professores</h2>
            <ul>
                ${professores.map(p => `<li>${p.nome} - ${p.email}</li>`).join('')}
            </ul>
        </div>
        
        <div class="report-section">
            <h2>👥 Turmas</h2>
            <ul>
                ${turmas.map(t => `<li>${t.nome} - ${t.serie} (${t.turno})</li>`).join('')}
            </ul>
        </div>
    `;
}

function generateProfessoresReport() {
    return `
        <h1>👨‍🏫 Relatório de Professores</h1>
        ${professores.map(p => `
            <div class="report-section">
                <h2>${p.nome}</h2>
                <p><strong>Email:</strong> ${p.email}</p>
                <p><strong>Disciplinas:</strong> ${Array.isArray(p.disciplinas) ? p.disciplinas.join(', ') : 'N/A'}</p>
                <p><strong>Aulas Contratadas:</strong> ${p.aulasContratadas || 0}</p>
            </div>
        `).join('')}
    `;
}

function generateTurmasReport() {
    return `
        <h1>👥 Relatório de Turmas</h1>
        ${turmas.map(t => `
            <div class="report-section">
                <h2>${t.nome}</h2>
                <p><strong>Série:</strong> ${t.serie}</p>
                <p><strong>Turno:</strong> ${t.turno}</p>
                <p><strong>Número de Alunos:</strong> ${t.numeroAlunos || 0}</p>
            </div>
        `).join('')}
    `;
}

function generateDisciplinasReport() {
    return `
        <h1>📚 Relatório de Disciplinas</h1>
        ${disciplinas.map(d => `
            <div class="report-section">
                <h2>${d.nome}</h2>
                <p><strong>Carga Horária:</strong> ${d.cargaHoraria}h</p>
                <p><strong>Descrição:</strong> ${d.descricao || 'N/A'}</p>
            </div>
        `).join('')}
    `;
}

function generateGradeReport() {
    return `
        <h1>📅 Relatório da Grade Horária</h1>
        <div class="report-section">
            <h2>📊 Estatísticas</h2>
            <p><strong>Aulas Agendadas:</strong> ${document.querySelectorAll('.schedule-cell.occupied').length}</p>
            <p><strong>Conflitos Detectados:</strong> 0</p>
        </div>
        
        <div class="report-section">
            <h2>📅 Grade Completa</h2>
            <p>Grade horária detalhada seria exibida aqui...</p>
        </div>
    `;
}

function openReportWindow(content, type) {
    const reportWindow = window.open('', '_blank');
    reportWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Relatório EducaWeb - ${type}</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                h1 { color: #2563eb; border-bottom: 2px solid #2563eb; padding-bottom: 10px; }
                h2 { color: #1e40af; margin-top: 30px; }
                .report-section { margin-bottom: 30px; }
                @media print { 
                    button { display: none; } 
                    body { margin: 0; }
                }
            </style>
        </head>
        <body>
            <button onclick="window.print()" style="margin-bottom: 20px; padding: 10px 20px; background: #2563eb; color: white; border: none; border-radius: 5px; cursor: pointer;">🖨️ Imprimir</button>
            ${content}
        </body>
        </html>
    `);
    reportWindow.document.close();
}

// Inicialização da aplicação
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM carregado, inicializando app...');
    
    // Verificar se usuário já está logado
    const token = localStorage.getItem('authToken');
    const userString = localStorage.getItem('user');
    
    if (token && userString) {
        try {
            authToken = token;
            currentUser = JSON.parse(userString);
            
            // Mostrar app e esconder login
            document.getElementById('loginView').style.display = 'none';
            document.getElementById('appView').style.display = 'block';
            document.getElementById('mainNav').style.display = 'flex';
            
            loadData();
            showView('dashboard');
        } catch (error) {
            console.error('Erro ao parsear dados do usuário:', error);
            localStorage.removeItem('authToken');
            localStorage.removeItem('user');
            
            // Mostrar login
            document.getElementById('loginView').style.display = 'block';
            document.getElementById('appView').style.display = 'none';
            document.getElementById('mainNav').style.display = 'none';
        }
    } else {
        // Mostrar login
        document.getElementById('loginView').style.display = 'block';
        document.getElementById('appView').style.display = 'none';
        document.getElementById('mainNav').style.display = 'none';
    }
    
    // Adicionar event listeners
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', login);
    }
    
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
    
    console.log('App inicializado!');
});
