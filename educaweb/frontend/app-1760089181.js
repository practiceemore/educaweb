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
            throw new Error(data.error || 'Erro na requisição');
        }

        return data;
    } catch (error) {
        console.error('Erro na API:', error);
        throw error;
    }
}

// ===== AUTENTICAÇÃO =====

async function login(email, password) {
    try {
        console.log("Enviando para API:", { email, password });
        
        const response = await apiRequest('/auth/login', 'POST', { email, password });

        if (response.success) {
            authToken = response.data.token;
            currentUser = response.data.user;
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            showNotification('Login realizado com sucesso!', 'success');
            showDashboard();
        showNavigation();
            return true;
        }
    } catch (error) {
        showNotification('Erro no login: ' + error.message, 'error');
        return false;
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    showNotification('Logout realizado com sucesso!', 'success');
    showLoginScreen();
    hideNavigation();
}

// ===== CARREGAMENTO DE DADOS =====

async function loadData() {
    try {
        const [disciplinasData, professoresData, turmasData, salasData] = await Promise.all([
            apiRequest('/disciplinas').then(r => r.success ? r.data : []).catch(() => []),
            apiRequest('/professores').then(r => r.success ? r.data : []).catch(() => []),
            apiRequest('/turmas').then(r => r.success ? r.data : []).catch(() => []),
            apiRequest('/salas').then(r => r.success ? r.data : []).catch(() => [])
        ]);

        disciplinas = disciplinasData;
        professores = professoresData;
        turmas = turmasData;
        salas = salasData;

        updateDashboard();
        showNotification('Dados carregados com sucesso!', 'success');
    } catch (error) {
        console.error('Erro ao carregar dados:', error);
        showNotification('Erro ao carregar dados: ' + error.message, 'error');
    }
}

// ===== INTERFACE =====

function showLoginScreen() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="login-container">
                <div class="login-card">
                    <h2>🎓 EducaWeb</h2>
                    <p>Faça login para acessar o sistema</p>
                    <form id="loginForm">
                        <div class="form-group">
                            <label for="email">Email:</label>
                            <input type="email" id="email" required>
                        </div>
                        <div class="form-group">
                            <label for="password">Senha:</label>
                            <input type="password" id="password" required>
                        </div>
                        <button type="submit" class="btn-primary">Entrar</button>
                    </form>
                    <div class="login-info">
                        <p><strong>Usuário padrão:</strong></p>
                        <p>Email: admin@educaweb.com</p>
                        <p>Senha: admin123</p>
                    </div>
                </div>
            </div>
        `;
        
        // Adicionar evento de login
        document.getElementById('loginForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            
            const success = await login(email, password);
            if (!success) {
                showNotification('Erro no login. Verifique suas credenciais.', 'error');
            }
        });
    }
}

function showDashboard() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="dashboard">
                <div class="dashboard-header">
                    <h1>📊 Dashboard</h1>
                    <div class="user-info">
                        <span class="user-name" id="userName">${currentUser ? currentUser.name : 'Usuário'}</span>
                        <button class="btn-secondary" onclick="logout()">Sair</button>
                    </div>
                </div>
                
                <div class="dashboard-stats">
                    <div class="stat-card">
                        <div class="stat-icon">📚</div>
                        <div class="stat-content">
                            <div class="stat-number" id="totalDisciplinas">${disciplinas.length}</div>
                            <div class="stat-label">Disciplinas</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">👨‍🏫</div>
                        <div class="stat-content">
                            <div class="stat-number" id="totalProfessores">${professores.length}</div>
                            <div class="stat-label">Professores</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">👥</div>
                        <div class="stat-content">
                            <div class="stat-number" id="totalTurmas">${turmas.length}</div>
                            <div class="stat-label">Turmas</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">🏫</div>
                        <div class="stat-content">
                            <div class="stat-number" id="totalSalas">${salas.length}</div>
                            <div class="stat-label">Salas</div>
                        </div>
                    </div>
                </div>
                
                <div class="dashboard-content">
                    <div class="welcome-message">
                        <h2>Bem-vindo ao EducaWeb!</h2>
                        <p>Sistema de gestão educacional com IA integrada.</p>
                        <p><strong>Status do Backend:</strong> <span id="backendStatus">Conectado</span></p>
                    </div>
                </div>
            </div>
        `;
        
        // Carregar dados após mostrar dashboard
        loadData();
    }
}

function updateDashboard() {
    const totalDisciplinas = document.getElementById('totalDisciplinas');
    const totalProfessores = document.getElementById('totalProfessores');
    const totalTurmas = document.getElementById('totalTurmas');
    const totalSalas = document.getElementById('totalSalas');
    
    if (totalDisciplinas) totalDisciplinas.textContent = disciplinas.length;
    if (totalProfessores) totalProfessores.textContent = professores.length;
    if (totalTurmas) totalTurmas.textContent = turmas.length;
    if (totalSalas) totalSalas.textContent = salas.length;
}

// ===== CRUD DISCIPLINAS =====

async function addDisciplina(data) {
    try {
        const response = await apiRequest('/disciplinas', 'POST', data);
        
        if (response.success) {
            disciplinas.push(response.data);
            updateDashboard();
            showNotification('Disciplina criada com sucesso!', 'success');
            return true;
        }
    } catch (error) {
        showNotification('Erro ao criar disciplina: ' + error.message, 'error');
        return false;
    }
}

async function updateDisciplina(id, data) {
    try {
        const response = await apiRequest(`/disciplinas/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        
        if (response.success) {
            const index = disciplinas.findIndex(d => d.id == id);
            if (index !== -1) {
                disciplinas[index] = response.data;
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
    if (!confirm("Tem certeza que deseja deletar esta disciplina?")) {
        return;
    }
    
    try {
        const response = await apiRequest(`/disciplinas/${id}`, 'DELETE');
        
        if (response.success) {
            const index = disciplinas.findIndex(d => d.id == id);
            if (index !== -1) {
                disciplinas.splice(index, 1);
                updateDashboard();
                showDisciplinasView(); // Atualizar a view atual
            }
            showNotification("Disciplina deletada com sucesso!", "success");
            return true;
        }
    } catch (error) {
        showNotification("Erro ao deletar disciplina: " + error.message, "error");
        return false;
    }
}

// ===== INICIALIZAÇÃO =====

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM carregado, inicializando app...');
    
    // Verificar se usuário já está logado
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('user');
    
    if (token && user) {
        authToken = token;
        try {
            currentUser = JSON.parse(user);
        } catch (e) {
            console.error("Erro ao fazer parse do usuário:", e);
            localStorage.removeItem("authToken");
            localStorage.removeItem("user");
            showLoginScreen();
            return;
        }
        showDashboard();
        showNavigation();
    } else {
        showLoginScreen();
    hideNavigation();
    }
});

console.log('🔗 EducaWeb carregado com sucesso!');

// ===== NAVEGAÇÃO =====

function showNavigation() {
    const nav = document.getElementById('mainNav');
    if (nav) {
        nav.style.display = 'block';
    }
}

function hideNavigation() {
    const nav = document.getElementById('mainNav');
    if (nav) {
        nav.style.display = 'none';
    }
}

function showView(viewName) {
    // Remover classe active de todos os botões
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Adicionar classe active ao botão clicado
    const activeBtn = document.querySelector(`[onclick="showView('${viewName}')"]`);
    if (activeBtn) {
        activeBtn.classList.add('active');
    }
    
    // Mostrar a view correspondente
    switch(viewName) {
        case 'dashboard':
            showDashboard();
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
            showGradeView();
            break;
        case 'ai':
            showAIView();
            break;
        case 'chat':
            showChatView();
            break;
        default:
            showDashboard();
    }
}

// ===== VIEWS =====

function showDisciplinasView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>📚 Gestão de Disciplinas</h2>
                    <button class="btn-primary" onclick="showAddDisciplinaModal()">➕ Adicionar Disciplina</button>
                </div>
                <div class="disciplinas-table-container">
                    <table class="disciplinas-table">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Descrição</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody id="disciplinasTableBody">
                            ${disciplinas.map(d => `
                                <tr>
                                    <td>${d.nome}</td>
                                    <td>${d.descricao || '-'}</td>
                                    <td>
                                        <div class="action-buttons">
                                            <button class="action-btn edit" onclick="editDisciplina('${d.id}')" title="Editar">✏️</button>
                                            <button class="action-btn delete" onclick="deleteDisciplina('${d.id}')" title="Excluir">🗑️</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
}

function showProfessoresView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>👨‍🏫 Gestão de Professores</h2>
                    <button class="btn-primary" onclick="showAddProfessorModal()">➕ Adicionar Professor</button>
                </div>
                <div class="professores-table-container">
                    <table class="professores-table">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Email</th>
                                <th>Telefone</th>
                                <th>Especialidade</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody id="professoresTableBody">
                            ${professores.map(p => `
                                <tr>
                                    <td>${p.nome}</td>
                                    <td>${p.email}</td>
                                    <td>${p.telefone || '-'}</td>
                                    <td>${p.especialidade || '-'}</td>
                                    <td>
                                        <div class="action-buttons">
                                            <button class="action-btn edit" onclick="editProfessor('${p.id}')" title="Editar">✏️</button>
                                            <button class="action-btn delete" onclick="deleteProfessor('${p.id}')" title="Excluir">🗑️</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
}

function showTurmasView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>👥 Gestão de Turmas</h2>
                    <button class="btn-primary" onclick="showAddTurmaModal()">➕ Adicionar Turma</button>
                </div>
                <div class="turmas-table-container">
                    <table class="turmas-table">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Série</th>
                                <th>Turno</th>
                                <th>Capacidade</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody id="turmasTableBody">
                            ${turmas.map(t => `
                                <tr>
                                    <td>${t.nome}</td>
                                    <td>${t.serie}</td>
                                    <td>${t.turno}</td>
                                    <td>${t.capacidade}</td>
                                    <td>
                                        <div class="action-buttons">
                                            <button class="action-btn edit" onclick="editTurma('${t.id}')" title="Editar">✏️</button>
                                            <button class="action-btn edit" onclick="showTurmaDisciplinasModal('${t.id}')" title="Disciplinas">📚</button>
                                            <button class="action-btn delete" onclick="deleteTurma('${t.id}')" title="Excluir">🗑️</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
}

function showSalasView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>🏫 Gestão de Salas</h2>
                    <button class="btn-primary" onclick="showAddSalaModal()">➕ Adicionar Sala</button>
                </div>
                <div class="salas-table-container">
                    <table class="salas-table">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Tipo</th>
                                <th>Capacidade</th>
                                <th>Equipamentos</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody id="salasTableBody">
                            ${salas.map(s => `
                                <tr>
                                    <td>${s.nome}</td>
                                    <td>${s.tipo}</td>
                                    <td>${s.capacidade}</td>
                                    <td>${s.recursos ? (Array.isArray(s.recursos) ? s.recursos.join(', ') : s.recursos) : '-'}</td>
                                    <td>
                                        <div class="action-buttons">
                                            <button class="action-btn edit" onclick="editSala('${s.id}')" title="Editar">✏️</button>
                                            <button class="action-btn delete" onclick="deleteSala('${s.id}')" title="Excluir">🗑️</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
}

function showGradeView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>📅 Grade Horária</h2>
                    <button class="btn-primary" onclick="showAddAulaModal()">➕ Adicionar Aula</button>
                </div>
                <div class="grade-container">
                    <p>Grade horária será implementada aqui...</p>
                </div>
            </div>
        `;
    }
}

function showAIView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>�� Assistente IA</h2>
                </div>
                <div class="ai-container">
                    <p>Assistente IA será implementado aqui...</p>
                </div>
            </div>
        `;
    }
}

// ===== MODAIS (PLACEHOLDERS) =====

function showAddDisciplinaModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addDisciplinaModal");
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.id = "addDisciplinaModal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>➕ Adicionar Nova Disciplina</h3>
                <span class="close" onclick="closeModal('addDisciplinaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addDisciplinaForm">
                    <div class="form-group">
                        <label for="disciplinaNome">Nome da Disciplina:</label>
                        <input type="text" id="disciplinaNome" required placeholder="Ex: Matemática, Português, História...">
                    </div>
                    <div class="form-group">
                        <label for="disciplinaDescricao">Descrição (opcional):</label>
                        <textarea id="disciplinaDescricao" placeholder="Descreva o conteúdo da disciplina..."></textarea>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addDisciplinaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="submitAddDisciplina()">Salvar Disciplina</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    modal.style.display = "block";
    
    // Focar no primeiro campo
    document.getElementById("disciplinaNome").focus();
    
    // Fechar modal ao clicar fora dele
    modal.onclick = function(event) {
        if (event.target === modal) {
            closeModal("addDisciplinaModal");
        }
    };
}

function submitAddDisciplina() {
    const nome = document.getElementById("disciplinaNome").value.trim();
    const descricao = document.getElementById("disciplinaDescricao").value.trim();
    
    // Validação
    if (!nome) {
        showNotification("Nome da disciplina é obrigatório!", "error");
        document.getElementById("disciplinaNome").focus();
        return;
    }
    
    const disciplinaData = {
        nome: nome,
        descricao: descricao || null
    };
    
    // Mostrar loading
    const submitBtn = document.querySelector("#addDisciplinaModal .btn-primary");
    const originalText = submitBtn.textContent;
    submitBtn.textContent = "Salvando...";
    submitBtn.disabled = true;
    
    addDisciplina(disciplinaData).then(success => {
        if (success) {
            closeModal("addDisciplinaModal");
            showDisciplinasView();
            showNotification("Disciplina adicionada com sucesso!", "success");
        }
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }).catch(error => {
        showNotification("Erro ao adicionar disciplina: " + error.message, "error");
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    });
}

function showAddProfessorModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addProfessorModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal
    const modal = document.createElement("div");
    modal.id = "addProfessorModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>👨‍🏫 Adicionar Professor</h3>
                <span class="close" onclick="closeModal('addProfessorModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addProfessorForm">
                    <div class="form-group">
                        <label for="professorNome">Nome:</label>
                        <input type="text" id="professorNome" name="nome" required>
                    </div>
                    <div class="form-group">
                        <label for="professorEmail">Email:</label>
                        <input type="email" id="professorEmail" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="professorTelefone">Telefone:</label>
                        <input type="tel" id="professorTelefone" name="telefone">
                    </div>
                    <div class="form-group">
                        <label for="professorEspecialidade">Especialidade:</label>
                        <input type="text" id="professorEspecialidade" name="especialidade" required>
                    </div>
                    <div class="form-group">
                        <label for="professorAulasContratadas">Aulas Contratadas:</label>
                        <input type="number" id="professorAulasContratadas" name="aulasContratadas" min="1" value="20" required>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addProfessorModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="saveProfessor()">Salvar Professor</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

function showAddTurmaModal() {
    showNotification('Modal de adicionar turma será implementado', 'info');
}

function showAddSalaModal() {
    showNotification('Modal de adicionar sala será implementado', 'info');
}

function showAddAulaModal() {
    showNotification('Modal de adicionar aula será implementado', 'info');
}

function editDisciplina(id) {
    showNotification(`Editando disciplina ${id}`, 'info');
}


function editProfessor(id) {

async function deleteProfessor(id) {
    if (!confirm("Tem certeza que deseja deletar este professor?")) {
        return;
    }
    
    try {
        const response = await apiRequest(`/professores/${id}`, 'DELETE');
        
        if (response.success) {
            const index = professores.findIndex(p => p.id == id);
            if (index !== -1) {
                professores.splice(index, 1);
                updateDashboard();
                showProfessoresView(); // Atualizar a view atual
            }
            showNotification("Professor deletado com sucesso!", "success");
            return true;
        }
    } catch (error) {
        showNotification("Erro ao deletar professor: " + error.message, "error");
        return false;
    }
}
    showNotification(`Editando professor ${id}`, 'info');
}


function editTurma(id) {
    showNotification(`Editando turma ${id}`, 'info');
}

function deleteTurma(id) {
    if (confirm('Tem certeza que deseja deletar esta turma?')) {
        showNotification(`Turma ${id} deletada`, 'success');
    }
}

function editSala(id) {
    showNotification(`Editando sala ${id}`, 'info');
}

function deleteSala(id) {
    if (confirm('Tem certeza que deseja deletar esta sala?')) {
        showNotification(`Sala ${id} deletada`, 'success');
    }
}


// Função para adicionar professor
function showAddProfessorModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addProfessorModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal
    const modal = document.createElement("div");
    modal.id = "addProfessorModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>👨‍🏫 Adicionar Professor</h3>
                <span class="close" onclick="closeModal('addProfessorModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addProfessorForm">
                    <div class="form-group">
                        <label for="professorNome">Nome:</label>
                        <input type="text" id="professorNome" name="nome" required>
                    </div>
                    <div class="form-group">
                        <label for="professorEmail">Email:</label>
                        <input type="email" id="professorEmail" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="professorTelefone">Telefone:</label>
                        <input type="tel" id="professorTelefone" name="telefone">
                    </div>
                    <div class="form-group">
                        <label for="professorEspecialidade">Especialidade:</label>
                        <input type="text" id="professorEspecialidade" name="especialidade" required>
                    </div>
                    <div class="form-group">
                        <label for="professorAulasContratadas">Aulas Contratadas:</label>
                        <input type="number" id="professorAulasContratadas" name="aulasContratadas" min="1" value="20" required>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addProfessorModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="saveProfessor()">Salvar Professor</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para salvar professor
async function saveProfessor() {
    const form = document.getElementById("addProfessorForm");
    const formData = new FormData(form);
    
    const professorData = {
        nome: formData.get("nome"),
        email: formData.get("email"),
        telefone: formData.get("telefone"),
        especialidade: formData.get("especialidade"),
        aulasContratadas: parseInt(formData.get("aulasContratadas"))
    };
    
    try {
        // Validação simples de email duplicado no client (melhor UX)
        const emailJaExiste = professores.some(p => (p.email || '').toLowerCase() === (professorData.email || '').toLowerCase());
        if (emailJaExiste) {
            showNotification("Já existe um professor com este e-mail.", "error");
            return;
        }

        const response = await apiRequest("/professores", "POST", professorData);
        
        if (response.success) {
            professores.push(response.data);
            updateDashboard();
            showProfessoresView();
            closeModal("addProfessorModal");
            showNotification("Professor adicionado com sucesso!", "success");
        } else {
            // Superfície mensagens amigáveis do backend (ex.: e-mail duplicado)
            const msg = response.error || "Erro ao adicionar professor";
            showNotification(msg, "error");
        }
    } catch (error) {
        // Tentar extrair mensagem clara do backend
        const serverMsg = (error && error.message) ? error.message : "Erro ao adicionar professor";
        showNotification(serverMsg, "error");
    }
}

// Função para editar professor
async function editProfessor(id) {
    const professor = professores.find(p => p.id == id);
    if (!professor) return;
    
    // Remover modal existente se houver
    const existingModal = document.getElementById("editProfessorModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal de edição
    const modal = document.createElement("div");
    modal.id = "editProfessorModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>✏️ Editar Professor</h3>
                <span class="close" onclick="closeModal('editProfessorModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="editProfessorForm">
                    <div class="form-group">
                        <label for="editProfessorNome">Nome:</label>
                        <input type="text" id="editProfessorNome" name="nome" value="${professor.nome}" required>
                    </div>
                    <div class="form-group">
                        <label for="editProfessorEmail">Email:</label>
                        <input type="email" id="editProfessorEmail" name="email" value="${professor.email}" required>
                    </div>
                    <div class="form-group">
                        <label for="editProfessorTelefone">Telefone:</label>
                        <input type="tel" id="editProfessorTelefone" name="telefone" value="${professor.telefone || ''}">
                    </div>
                    <div class="form-group">
                        <label for="editProfessorEspecialidade">Especialidade:</label>
                        <input type="text" id="editProfessorEspecialidade" name="especialidade" value="${professor.especialidade}" required>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('editProfessorModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="updateProfessor(${id})">Atualizar Professor</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para atualizar professor
async function updateProfessor(id) {
    const form = document.getElementById("editProfessorForm");
    const formData = new FormData(form);
    
    const professorData = {
        nome: formData.get("nome"),
        email: formData.get("email"),
        telefone: formData.get("telefone"),
        especialidade: formData.get("especialidade")
    };
    
    try {
        const response = await apiRequest(`/professores/${id}`, {
            method: "PUT",
            body: JSON.stringify(professorData)
        });
        
        if (response.success) {
            const index = professores.findIndex(p => p.id == id);
            if (index !== -1) {
                professores[index] = response.data;
                updateDashboard();
                showProfessoresView();
                closeModal("editProfessorModal");
                showNotification("Professor atualizado com sucesso!", "success");
            }
        }
    } catch (error) {
        showNotification("Erro ao atualizar professor: " + error.message, "error");
    }
}

// Função para deletar professor
async function deleteProfessor(id) {
    if (!confirm("Tem certeza que deseja deletar este professor?")) {
        return;
    }
    
    try {
        const response = await apiRequest(`/professores/${id}`, 'DELETE');
        
        if (response.success) {
            const index = professores.findIndex(p => p.id == id);
            if (index !== -1) {
                professores.splice(index, 1);
                updateDashboard();
                showProfessoresView();
            }
            showNotification("Professor deletado com sucesso!", "success");
            return true;
        }
    } catch (error) {
        showNotification("Erro ao deletar professor: " + error.message, "error");
        return false;
    }
}

// ===== MODAIS PARA TURMAS =====

// Função para adicionar turma
function showAddTurmaModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addTurmaModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal
    const modal = document.createElement("div");
    modal.id = "addTurmaModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>➕ Adicionar Turma</h3>
                <span class="close" onclick="closeModal('addTurmaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addTurmaForm">
                    <div class="form-group">
                        <label for="turmaNome">Nome da Turma:</label>
                        <input type="text" id="turmaNome" name="nome" required>
                    </div>
                    <div class="form-group">
                        <label for="turmaSerie">Série:</label>
                        <input type="text" id="turmaSerie" name="serie" required>
                    </div>
                    <div class="form-group">
                        <label for="turmaTurno">Turno:</label>
                        <select id="turmaTurno" name="turno" required>
                            <option value="">Selecione o turno</option>
                            <option value="Manhã">Manhã</option>
                            <option value="Tarde">Tarde</option>
                            <option value="Noite">Noite</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="turmaCapacidade">Capacidade:</label>
                        <input type="number" id="turmaCapacidade" name="capacidade" min="1" required>
                    </div>
                    <div class="form-group">
                        <label for="turmaAnoLetivo">Ano Letivo:</label>
                        <input type="text" id="turmaAnoLetivo" name="anoLetivo" value="2024" required>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addTurmaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="saveTurma()">Salvar Turma</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para salvar turma
async function saveTurma() {
    const form = document.getElementById("addTurmaForm");
    const formData = new FormData(form);
    
    const turmaData = {
        anoLetivo: formData.get("anoLetivo"),
        nome: formData.get("nome"),
        serie: formData.get("serie"),
        turno: formData.get("turno"),
        capacidade: parseInt(formData.get("capacidade"))
    };
    
    try {
        const response = await apiRequest("/turmas", "POST", turmaData);
        
        if (response.success) {
            turmas.push(response.data);
            updateDashboard();
            showTurmasView();
            closeModal("addTurmaModal");
            showNotification("Turma adicionada com sucesso!", "success");
        }
    } catch (error) {
        showNotification("Erro ao adicionar turma: " + error.message, "error");
    }
}

// Função para editar turma
async function editTurma(id) {
    const turma = turmas.find(t => t.id == id);
    if (!turma) return;
    
    // Remover modal existente se houver
    const existingModal = document.getElementById("editTurmaModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal de edição
    const modal = document.createElement("div");
    modal.id = "editTurmaModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>✏️ Editar Turma</h3>
                <span class="close" onclick="closeModal('editTurmaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="editTurmaForm">
                    <div class="form-group">
                        <label for="editTurmaNome">Nome da Turma:</label>
                        <input type="text" id="editTurmaNome" name="nome" value="${turma.nome}" required>
                    </div>
                    <div class="form-group">
                        <label for="editTurmaSerie">Série:</label>
                        <input type="text" id="editTurmaSerie" name="serie" value="${turma.serie}" required>
                    </div>
                    <div class="form-group">
                        <label for="editTurmaTurno">Turno:</label>
                        <select id="editTurmaTurno" name="turno" required>
                            <option value="">Selecione o turno</option>
                            <option value="Manhã" ${turma.turno === 'Manhã' ? 'selected' : ''}>Manhã</option>
                            <option value="Tarde" ${turma.turno === 'Tarde' ? 'selected' : ''}>Tarde</option>
                            <option value="Noite" ${turma.turno === 'Noite' ? 'selected' : ''}>Noite</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="editTurmaCapacidade">Capacidade:</label>
                        <input type="number" id="editTurmaCapacidade" name="capacidade" value="${turma.capacidade}" min="1" required>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('editTurmaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="updateTurma(${id})">Atualizar Turma</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para atualizar turma
async function updateTurma(id) {
    const form = document.getElementById("editTurmaForm");
    const formData = new FormData(form);
    
    const turmaData = {
        nome: formData.get("nome"),
        serie: formData.get("serie"),
        turno: formData.get("turno"),
        capacidade: parseInt(formData.get("capacidade"))
    };
    
    try {
        const response = await apiRequest(`/turmas/${id}`, {
            method: "PUT",
            body: JSON.stringify(turmaData)
        });
        
        if (response.success) {
            const index = turmas.findIndex(t => t.id == id);
            if (index !== -1) {
                turmas[index] = response.data;
                updateDashboard();
                showTurmasView();
                closeModal("editTurmaModal");
                showNotification("Turma atualizada com sucesso!", "success");
            }
        }
    } catch (error) {
        showNotification("Erro ao atualizar turma: " + error.message, "error");
    }
}

// Função para deletar turma
async function deleteTurma(id) {
    if (!confirm("Tem certeza que deseja deletar esta turma?")) {
        return;
    }
    
    try {
        const response = await apiRequest(`/turmas/${id}`, 'DELETE');
        
        if (response.success) {
            const index = turmas.findIndex(t => t.id == id);
            if (index !== -1) {
                turmas.splice(index, 1);
                updateDashboard();
                showTurmasView();
            }
            showNotification("Turma deletada com sucesso!", "success");
            return true;
        }
    } catch (error) {
        showNotification("Erro ao deletar turma: " + error.message, "error");
        return false;
    }
}

// ===== MODAIS PARA SALAS =====

// Função para adicionar sala
function showAddSalaModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addSalaModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal
    const modal = document.createElement("div");
    modal.id = "addSalaModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>➕ Adicionar Sala</h3>
                <span class="close" onclick="closeModal('addSalaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addSalaForm">
                    <div class="form-group">
                        <label for="salaNumero">Número da Sala:</label>
                        <input type="text" id="salaNumero" name="numero" required>
                    </div>
                    <div class="form-group">
                        <label for="salaNome">Nome da Sala:</label>
                        <input type="text" id="salaNome" name="nome" required>
                    </div>
                    <div class="form-group">
                        <label for="salaCapacidade">Capacidade:</label>
                        <input type="number" id="salaCapacidade" name="capacidade" min="1" required>
                    </div>
                    <div class="form-group">
                        <label for="salaTipo">Tipo:</label>
                        <select id="salaTipo" name="tipo" required>
                            <option value="">Selecione o tipo</option>
                            <option value="Sala de Aula">Sala de Aula</option>
                            <option value="Laboratório">Laboratório</option>
                            <option value="Auditório">Auditório</option>
                            <option value="Biblioteca">Biblioteca</option>
                            <option value="Sala de Reunião">Sala de Reunião</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="salaEquipamentos">Equipamentos:</label>
                        <textarea id="salaEquipamentos" name="equipamentos" rows="3" placeholder="Ex: Projetor, Quadro, Ar condicionado..."></textarea>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addSalaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="saveSala()">Salvar Sala</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para salvar sala
async function saveSala() {
    const form = document.getElementById("addSalaForm");
    const formData = new FormData(form);
    
    const salaData = {
        numero: formData.get("numero"),
        nome: formData.get("nome"),
        capacidade: parseInt(formData.get("capacidade")),
        tipo: formData.get("tipo"),
        equipamentos: formData.get("equipamentos") || null
    };
    
    try {
        const response = await apiRequest("/salas", "POST", salaData);
        
        if (response.success) {
            salas.push(response.data);
            updateDashboard();
            showSalasView();
            closeModal("addSalaModal");
            showNotification("Sala adicionada com sucesso!", "success");
        }
    } catch (error) {
        showNotification("Erro ao adicionar sala: " + error.message, "error");
    }
}

// Função para editar sala
async function editSala(id) {
    const sala = salas.find(s => s.id == id);
    if (!sala) return;
    
    // Remover modal existente se houver
    const existingModal = document.getElementById("editSalaModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Criar modal de edição
    const modal = document.createElement("div");
    modal.id = "editSalaModal";
    modal.className = "modal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>✏️ Editar Sala</h3>
                <span class="close" onclick="closeModal('editSalaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="editSalaForm">
                    <div class="form-group">
                        <label for="editSalaNumero">Número da Sala:</label>
                        <input type="text" id="editSalaNumero" name="numero" value="${sala.numero}" required>
                    </div>
                    <div class="form-group">
                        <label for="editSalaNome">Nome da Sala:</label>
                        <input type="text" id="editSalaNome" name="nome" value="${sala.nome}" required>
                    </div>
                    <div class="form-group">
                        <label for="editSalaCapacidade">Capacidade:</label>
                        <input type="number" id="editSalaCapacidade" name="capacidade" value="${sala.capacidade}" min="1" required>
                    </div>
                    <div class="form-group">
                        <label for="editSalaTipo">Tipo:</label>
                        <select id="editSalaTipo" name="tipo" required>
                            <option value="">Selecione o tipo</option>
                            <option value="Sala de Aula" ${sala.tipo === 'Sala de Aula' ? 'selected' : ''}>Sala de Aula</option>
                            <option value="Laboratório" ${sala.tipo === 'Laboratório' ? 'selected' : ''}>Laboratório</option>
                            <option value="Auditório" ${sala.tipo === 'Auditório' ? 'selected' : ''}>Auditório</option>
                            <option value="Biblioteca" ${sala.tipo === 'Biblioteca' ? 'selected' : ''}>Biblioteca</option>
                            <option value="Sala de Reunião" ${sala.tipo === 'Sala de Reunião' ? 'selected' : ''}>Sala de Reunião</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="editSalaEquipamentos">Equipamentos:</label>
                        <textarea id="editSalaEquipamentos" name="equipamentos" rows="3" placeholder="Ex: Projetor, Quadro, Ar condicionado...">${sala.equipamentos || ''}</textarea>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('editSalaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="updateSala(${id})">Atualizar Sala</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";
}

// Função para atualizar sala
async function updateSala(id) {
    const form = document.getElementById("editSalaForm");
    const formData = new FormData(form);
    
    const salaData = {
        numero: formData.get("numero"),
        nome: formData.get("nome"),
        capacidade: parseInt(formData.get("capacidade")),
        tipo: formData.get("tipo"),
        equipamentos: formData.get("equipamentos") || null
    };
    
    try {
        const response = await apiRequest(`/salas/${id}`, {
            method: "PUT",
            body: JSON.stringify(salaData)
        });
        
        if (response.success) {
            const index = salas.findIndex(s => s.id == id);
            if (index !== -1) {
                salas[index] = response.data;
                updateDashboard();
                showSalasView();
                closeModal("editSalaModal");
                showNotification("Sala atualizada com sucesso!", "success");
            }
        }
    } catch (error) {
        showNotification("Erro ao atualizar sala: " + error.message, "error");
    }
}

// Função para deletar sala
async function deleteSala(id) {
    if (!confirm("Tem certeza que deseja deletar esta sala?")) {
        return;
    }
    
    try {
        const response = await apiRequest(`/salas/${id}`, 'DELETE');
        
        if (response.success) {
            const index = salas.findIndex(s => s.id == id);
            if (index !== -1) {
                salas.splice(index, 1);
                updateDashboard();
                showSalasView();
            }
            showNotification("Sala deletada com sucesso!", "success");
            return true;
        }
    } catch (error) {
        showNotification("Erro ao deletar sala: " + error.message, "error");
        return false;
    }
}

// ===== FUNÇÕES DO CHAT =====

// Variáveis do chat
let chatMessages = [];
let isLoadingMessage = false;

// Mostrar view do chat
function showChatView() {
    const main = document.querySelector('.main');
    main.innerHTML = `
        <div class="view-header">
            <h2>💬 Chat com IA</h2>
            <div class="view-actions">
                <button class="btn btn-secondary" onclick="clearChatHistory()">🗑️ Limpar Histórico</button>
            </div>
        </div>
        
        <div class="chat-container">
            <div class="chat-messages" id="chatMessages">
                <div class="chat-welcome">
                    <div class="welcome-icon">🤖</div>
                    <h3>Olá! Sou seu assistente educacional</h3>
                    <p>Posso ajudar você com:</p>
                    <ul>
                        <li>📊 Análises da grade horária</li>
                        <li>👥 Gestão de turmas e professores</li>
                        <li>📈 Relatórios e estatísticas</li>
                        <li>💡 Sugestões de otimização</li>
                        <li>❓ Dúvidas sobre o sistema</li>
                    </ul>
                    <p>Como posso ajudar você hoje?</p>
                </div>
            </div>
            
            <div class="chat-input-container">
                <div class="chat-input-wrapper">
                    <input type="text" id="chatInput" placeholder="Digite sua mensagem..." maxlength="500">
                    <button id="sendButton" onclick="sendChatMessage()" disabled>
                        <span class="send-icon">📤</span>
                    </button>
                </div>
                <div class="chat-input-footer">
                    <small>Pressione Enter para enviar • Shift+Enter para nova linha</small>
                </div>
            </div>
        </div>
    `;
    
    // Carregar histórico de mensagens
    loadChatHistory();
    
    // Configurar eventos
    setupChatEvents();
}

// Configurar eventos do chat
function setupChatEvents() {
    const chatInput = document.getElementById('chatInput');
    const sendButton = document.getElementById('sendButton');
    
    // Evento de digitação
    chatInput.addEventListener('input', function() {
        const hasText = this.value.trim().length > 0;
        sendButton.disabled = !hasText || isLoadingMessage;
    });
    
    // Evento de teclado
    chatInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!sendButton.disabled) {
                sendChatMessage();
            }
        }
    });
    
    // Focar no input
    chatInput.focus();
}

// Enviar mensagem do chat
async function sendChatMessage() {
    const chatInput = document.getElementById('chatInput');
    const sendButton = document.getElementById('sendButton');
    const message = chatInput.value.trim();
    
    if (!message || isLoadingMessage) return;
    
    // Limpar input e desabilitar botão
    chatInput.value = '';
    sendButton.disabled = true;
    isLoadingMessage = true;
    
    // Adicionar mensagem do usuário
    addChatMessage(message, 'user');
    
    // Mostrar indicador de carregamento
    const loadingId = addChatMessage('🤖 Pensando...', 'assistant', true);
    
    try {
        const response = await apiRequest('/chat/send', 'POST', {
            mensagem: message,
            metadata: {
                timestamp: new Date().toISOString(),
                userAgent: navigator.userAgent
            }
        });
        
        if (response.success) {
            // Remover indicador de carregamento
            removeChatMessage(loadingId);
            
            // Adicionar resposta da IA
            const aiResponse = response.data.aiMessage?.mensagem || response.data.resposta || 'Resposta não disponível';
            addChatMessage(aiResponse, 'assistant');
            
            // Salvar no histórico local
            chatMessages.push({
                id: response.data.id,
                mensagem: message,
                resposta: response.data.resposta,
                timestamp: response.data.timestamp
            });
            
            // Verificar se uma grade foi gerada
            if (response.data.gradeGenerated) {
                showNotification('Grade horária gerada! Redirecionando...', 'success');
                // Aguardar um pouco para o usuário ver a notificação
                setTimeout(() => {
                    showView('grade');
                }, 2000);
            }
        } else {
            throw new Error(response.error || 'Erro ao enviar mensagem');
        }
        
    } catch (error) {
        console.error('Erro ao enviar mensagem:', error);
        
        // Remover indicador de carregamento
        removeChatMessage(loadingId);
        
        // Mostrar erro
        addChatMessage('❌ Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente.', 'assistant');
    } finally {
        isLoadingMessage = false;
        sendButton.disabled = false;
        chatInput.focus();
    }
}

// Adicionar mensagem ao chat
function addChatMessage(content, sender, isLoading = false) {
    const chatMessages = document.getElementById('chatMessages');
    const messageId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    
    // Remover mensagem de boas-vindas se existir
    const welcome = chatMessages.querySelector('.chat-welcome');
    if (welcome) {
        welcome.remove();
    }
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message ${sender}`;
    messageDiv.id = messageId;
    
    if (isLoading) {
        messageDiv.innerHTML = `
            <div class="message-content loading">
                <div class="loading-dots">
                    <span></span><span></span><span></span>
                </div>
                <span class="loading-text">${content}</span>
            </div>
        `;
    } else {
        const timestamp = new Date().toLocaleTimeString('pt-BR', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-text">${formatChatMessage(content)}</div>
                <div class="message-time">${timestamp}</div>
            </div>
        `;
    }
    
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    return messageId;
}

// Remover mensagem do chat
function removeChatMessage(messageId) {
    const message = document.getElementById(messageId);
    if (message) {
        message.remove();
    }
}

// Formatar mensagem do chat
function formatChatMessage(content) {
    // Converter quebras de linha
    content = content.replace(/\n/g, '<br>');
    
    // Destacar listas
    content = content.replace(/\n• /g, '<br>• ');
    content = content.replace(/\n- /g, '<br>- ');
    
    // Destacar números
    content = content.replace(/(\d+)/g, '<strong>$1</strong>');
    
    return content;
}

// Carregar histórico do chat
async function loadChatHistory() {
    try {
        const response = await apiRequest('/chat/messages?limit=20', 'GET');
        
        if (response.success && response.data.length > 0) {
            const chatMessages = document.getElementById('chatMessages');
            
            // Remover mensagem de boas-vindas
            const welcome = chatMessages.querySelector('.chat-welcome');
            if (welcome) {
                welcome.remove();
            }
            
            // Adicionar mensagens do histórico
            response.data.reverse().forEach(msg => {
                addChatMessage(msg.mensagem, 'user');
                if (msg.resposta) {
                    addChatMessage(msg.resposta, 'assistant');
                }
            });
            
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }
    } catch (error) {
        console.error('Erro ao carregar histórico do chat:', error);
    }
}

// Limpar histórico do chat
async function clearChatHistory() {
    if (!confirm('Tem certeza que deseja limpar todo o histórico do chat?')) {
        return;
    }
    
    try {
        const response = await apiRequest('/chat/clear', 'DELETE');
        
        if (response.success) {
            // Limpar interface
            const chatMessages = document.getElementById('chatMessages');
            chatMessages.innerHTML = `
                <div class="chat-welcome">
                    <div class="welcome-icon">🤖</div>
                    <h3>Histórico limpo!</h3>
                    <p>Como posso ajudar você hoje?</p>
                </div>
            `;
            
            // Limpar array local
            chatMessages.length = 0;
            
            showNotification('Histórico do chat limpo com sucesso!', 'success');
        } else {
            throw new Error(response.error || 'Erro ao limpar histórico');
        }
    } catch (error) {
        console.error('Erro ao limpar histórico:', error);
        showNotification('Erro ao limpar histórico do chat', 'error');
    }
}

// ===== GESTÃO DE DISCIPLINAS DA TURMA =====

// Variável global para armazenar disciplinas da turma
let turmaDisciplinas = [];
let turmaAtual = null;

// Função para mostrar modal de disciplinas da turma
async function showTurmaDisciplinasModal(turmaId) {
    turmaAtual = turmas.find(t => t.id == turmaId);
    if (!turmaAtual) {
        showNotification('Turma não encontrada', 'error');
        return;
    }

    // Remover modal existente se houver
    const existingModal = document.getElementById("turmaDisciplinasModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Carregar disciplinas da turma
    await loadTurmaDisciplinas(turmaId);

    // Criar modal
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.id = "turmaDisciplinasModal";
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 800px;">
            <div class="modal-header">
                <h3>📚 Disciplinas da Turma: ${turmaAtual.nome}</h3>
                <span class="close" onclick="closeModal('turmaDisciplinasModal')">&times;</span>
            </div>
            <div class="modal-body">
                <div style="margin-bottom: 20px;">
                    <button class="btn-primary" onclick="showAddDisciplinaToTurmaModal()">➕ Adicionar Disciplina</button>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Disciplina</th>
                                <th>Aulas por Semana</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody id="turmaDisciplinasTableBody">
                            ${turmaDisciplinas.map(td => `
                                <tr>
                                    <td>${td.disciplina.nome}</td>
                                    <td>
                                        <input type="number" 
                                               id="aulas-${td.id}" 
                                               value="${td.aulasPorSemana}" 
                                               min="1" 
                                               max="20" 
                                               style="width: 80px; padding: 5px;"
                                               onchange="updateAulasPorSemana(${td.id}, this.value)">
                                    </td>
                                    <td>
                                        <button class="btn-delete" onclick="removeDisciplinaFromTurma(${td.id})">🗑️</button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";

    // Fechar modal ao clicar fora dele
    modal.onclick = function(event) {
        if (event.target === modal) {
            closeModal("turmaDisciplinasModal");
        }
    };
}

// Função para carregar disciplinas da turma
async function loadTurmaDisciplinas(turmaId) {
    try {
        const response = await apiRequest(`/turma-disciplinas/turma/${turmaId}`, 'GET');
        if (response.success) {
            turmaDisciplinas = response.data;
        } else {
            throw new Error(response.error || 'Erro ao carregar disciplinas');
        }
    } catch (error) {
        console.error('Erro ao carregar disciplinas da turma:', error);
        showNotification('Erro ao carregar disciplinas da turma', 'error');
        turmaDisciplinas = [];
    }
}

// Função para atualizar aulas por semana
async function updateAulasPorSemana(turmaDisciplinaId, novasAulas) {
    const aulas = parseInt(novasAulas);
    if (aulas < 1 || aulas > 20) {
        showNotification('Aulas por semana deve ser entre 1 e 20', 'error');
        return;
    }

    try {
        const response = await apiRequest(`/turma-disciplinas/${turmaDisciplinaId}`, 'PUT', {
            aulasPorSemana: aulas
        });

        if (response.success) {
            // Atualizar dados locais
            const index = turmaDisciplinas.findIndex(td => td.id === turmaDisciplinaId);
            if (index !== -1) {
                turmaDisciplinas[index].aulasPorSemana = aulas;
            }
            showNotification('Aulas por semana atualizadas!', 'success');
        } else {
            throw new Error(response.error || 'Erro ao atualizar');
        }
    } catch (error) {
        console.error('Erro ao atualizar aulas por semana:', error);
        showNotification('Erro ao atualizar aulas por semana', 'error');
    }
}

// Função para remover disciplina da turma
async function removeDisciplinaFromTurma(turmaDisciplinaId) {
    if (!confirm("Tem certeza que deseja remover esta disciplina da turma?")) {
        return;
    }

    try {
        const response = await apiRequest(`/turma-disciplinas/${turmaDisciplinaId}`, 'DELETE');

        if (response.success) {
            // Remover dos dados locais
            turmaDisciplinas = turmaDisciplinas.filter(td => td.id !== turmaDisciplinaId);
            
            // Recarregar modal
            await showTurmaDisciplinasModal(turmaAtual.id);
            showNotification('Disciplina removida da turma!', 'success');
        } else {
            throw new Error(response.error || 'Erro ao remover');
        }
    } catch (error) {
        console.error('Erro ao remover disciplina da turma:', error);
        showNotification('Erro ao remover disciplina da turma', 'error');
    }
}

// Função para mostrar modal de adicionar disciplina à turma
async function showAddDisciplinaToTurmaModal() {
    // Remover modal existente se houver
    const existingModal = document.getElementById("addDisciplinaToTurmaModal");
    if (existingModal) {
        existingModal.remove();
    }

    // Carregar disciplinas disponíveis
    let disciplinasDisponiveis = [];
    try {
        const response = await apiRequest(`/turma-disciplinas/disciplinas-disponiveis/${turmaAtual.id}`, 'GET');
        if (response.success) {
            disciplinasDisponiveis = response.data;
        }
    } catch (error) {
        console.error('Erro ao carregar disciplinas disponíveis:', error);
        showNotification('Erro ao carregar disciplinas disponíveis', 'error');
    }

    if (disciplinasDisponiveis.length === 0) {
        showNotification('Não há disciplinas disponíveis para adicionar', 'info');
        return;
    }

    // Criar modal
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.id = "addDisciplinaToTurmaModal";
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>➕ Adicionar Disciplina à Turma</h3>
                <span class="close" onclick="closeModal('addDisciplinaToTurmaModal')">&times;</span>
            </div>
            <div class="modal-body">
                <form id="addDisciplinaToTurmaForm">
                    <div class="form-group">
                        <label for="disciplinaSelect">Disciplina:</label>
                        <select id="disciplinaSelect" required>
                            <option value="">Selecione uma disciplina</option>
                            ${disciplinasDisponiveis.map(d => `
                                <option value="${d.id}">${d.nome}</option>
                            `).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="aulasPorSemana">Aulas por Semana:</label>
                        <input type="number" id="aulasPorSemana" required min="1" max="20" value="2">
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('addDisciplinaToTurmaModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="addDisciplinaToTurma()">Adicionar</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";

    // Fechar modal ao clicar fora dele
    modal.onclick = function(event) {
        if (event.target === modal) {
            closeModal("addDisciplinaToTurmaModal");
        }
    };
}

// Função para adicionar disciplina à turma
async function addDisciplinaToTurma() {
    const disciplinaId = document.getElementById("disciplinaSelect").value;
    const aulasPorSemana = parseInt(document.getElementById("aulasPorSemana").value);

    if (!disciplinaId) {
        showNotification("Selecione uma disciplina", "error");
        return;
    }

    if (!aulasPorSemana || aulasPorSemana < 1 || aulasPorSemana > 20) {
        showNotification("Aulas por semana deve ser entre 1 e 20", "error");
        return;
    }

    try {
        const response = await apiRequest("/turma-disciplinas", "POST", {
            turmaId: turmaAtual.id,
            disciplinaId: parseInt(disciplinaId),
            aulasPorSemana: aulasPorSemana
        });

        if (response.success) {
            closeModal("addDisciplinaToTurmaModal");
            // Recarregar modal de disciplinas
            await showTurmaDisciplinasModal(turmaAtual.id);
            showNotification("Disciplina adicionada à turma!", "success");
        } else {
            throw new Error(response.error || 'Erro ao adicionar disciplina');
        }
    } catch (error) {
        console.error('Erro ao adicionar disciplina à turma:', error);
        showNotification('Erro ao adicionar disciplina à turma', 'error');
    }
}

// ===== GRADE HORÁRIA =====

// Variáveis globais para grade horária
let gradeHoraria = [];
let turmasGrade = [];

// Função para mostrar view da grade horária
function showGradeView() {
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.innerHTML = `
            <div class="view-container">
                <div class="view-header">
                    <h2>📅 Grade Horária</h2>
                    <div>
                        <button class="btn-primary" onclick="showGenerateGradeModal()">🤖 Gerar Grade com IA</button>
                        <button class="btn-secondary" onclick="clearAllGrades()">🗑️ Limpar Grades</button>
                    </div>
                </div>
                <div id="gradeContainer">
                    <div class="loading">Carregando grade horária...</div>
                </div>
            </div>
        `;
        
        loadGradeHoraria();
    }
}

// Função para carregar grade horária
async function loadGradeHoraria() {
    try {
        const response = await apiRequest('/grade-horaria', 'GET');
        if (response.success) {
            gradeHoraria = response.data;
            turmasGrade = [...new Set(gradeHoraria.map(g => g.turma.id))];
            renderGradeHoraria();
        } else {
            throw new Error(response.error || 'Erro ao carregar grade horária');
        }
    } catch (error) {
        console.error('Erro ao carregar grade horária:', error);
        showNotification('Erro ao carregar grade horária', 'error');
        document.getElementById('gradeContainer').innerHTML = '<div class="error">Erro ao carregar grade horária</div>';
    }
}

// Função para renderizar grade horária
function renderGradeHoraria() {
    const container = document.getElementById('gradeContainer');
    
    if (gradeHoraria.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>📅 Nenhuma grade horária encontrada</h3>
                <p>Use a IA para gerar uma grade horária automaticamente ou adicione aulas manualmente.</p>
                <button class="btn-primary" onclick="showGenerateGradeModal()">🤖 Gerar Grade com IA</button>
            </div>
        `;
        return;
    }

    // Agrupar por turma
    const gradePorTurma = {};
    gradeHoraria.forEach(aula => {
        if (!gradePorTurma[aula.turma.id]) {
            gradePorTurma[aula.turma.id] = {
                turma: aula.turma,
                aulas: []
            };
        }
        gradePorTurma[aula.turma.id].aulas.push(aula);
    });

    // Renderizar grade para cada turma
    let html = '';
    Object.values(gradePorTurma).forEach(turmaData => {
        html += renderGradeTurma(turmaData.turma, turmaData.aulas);
    });

    container.innerHTML = html;
}

// Função para renderizar grade de uma turma específica
function renderGradeTurma(turma, aulas) {
    const diasSemana = ['segunda', 'terca', 'quarta', 'quinta', 'sexta'];
    const horarios = ['07:00', '08:00', '09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00'];
    
    // Criar matriz de grade
    const grade = {};
    diasSemana.forEach(dia => {
        grade[dia] = {};
        horarios.forEach(horario => {
            grade[dia][horario] = null;
        });
    });

    // Preencher com aulas existentes
    console.log('🔍 Renderizando grade para turma:', turma.nome, 'com', aulas.length, 'aulas');
    aulas.forEach(aula => {
        console.log('📚 Aula:', aula.diaSemana, aula.horarioInicio, aula.disciplina.nome);
        if (grade[aula.diaSemana] && grade[aula.diaSemana][aula.horarioInicio] === null) {
            grade[aula.diaSemana][aula.horarioInicio] = aula;
            console.log('✅ Aula adicionada à grade');
        } else {
            console.log('❌ Não foi possível adicionar aula:', aula.diaSemana, aula.horarioInicio);
        }
    });

    let html = `
        <div class="grade-turma">
            <h3>📚 ${turma.nome} - ${turma.serie}º ano</h3>
            <div class="grade-table-container">
                <table class="grade-table">
                    <thead>
                        <tr>
                            <th>Horário</th>
                            ${diasSemana.map(dia => `<th>${dia.charAt(0).toUpperCase() + dia.slice(1)}</th>`).join('')}
                        </tr>
                    </thead>
                    <tbody>
    `;

    horarios.forEach(horario => {
        html += `<tr><td class="horario">${horario}</td>`;
        diasSemana.forEach(dia => {
            const aula = grade[dia][horario];
            if (aula) {
                html += `
                    <td class="aula" onclick="editAulaGrade(${aula.id})">
                        <div class="aula-content">
                            <strong>${aula.disciplina.nome}</strong>
                            <br>
                            <small>${aula.professor.nome}</small>
                            <br>
                            <small>${aula.sala.nome}</small>
                        </div>
                    </td>
                `;
            } else {
                html += `<td class="vazio" onclick="addAulaGrade('${turma.id}', '${dia}', '${horario}')">+</td>`;
            }
        });
        html += '</tr>';
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
    `;

    return html;
}

// Função para mostrar modal de geração de grade com IA
function showGenerateGradeModal() {
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.id = "generateGradeModal";
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 600px;">
            <div class="modal-header">
                <h3>🤖 Gerar Grade Horária com IA</h3>
                <span class="close" onclick="closeModal('generateGradeModal')">&times;</span>
            </div>
            <div class="modal-body">
                <p>A IA irá analisar todos os dados da escola e gerar uma grade horária otimizada, evitando conflitos de professores e salas.</p>
                <div class="form-group">
                    <label>
                        <input type="checkbox" id="clearExisting" checked>
                        Limpar grade existente antes de gerar nova
                    </label>
                </div>
                <div class="form-group">
                    <label>Turma específica (opcional):</label>
                    <select id="turmaSelect">
                        <option value="">Todas as turmas</option>
                        ${turmas.map(t => `<option value="${t.id}">${t.nome} - ${t.serie}º ano</option>`).join('')}
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-secondary" onclick="closeModal('generateGradeModal')">Cancelar</button>
                <button type="button" class="btn-primary" onclick="generateGradeWithAI()">🤖 Gerar Grade</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = "block";

    modal.onclick = function(event) {
        if (event.target === modal) {
            closeModal("generateGradeModal");
        }
    };
}

// Função para gerar grade com IA
async function generateGradeWithAI() {
    const clearExisting = document.getElementById("clearExisting").checked;
    const turmaId = document.getElementById("turmaSelect").value;

    try {
        // Limpar grade existente se solicitado
        if (clearExisting) {
            const turmasToClear = turmaId ? [turmaId] : turmas.map(t => t.id);
            for (const tId of turmasToClear) {
                const response = await apiRequest(`/grade-horaria/turma/${tId}`, 'GET');
                if (response.success) {
                    for (const aula of response.data) {
                        await apiRequest(`/grade-horaria/${aula.id}`, 'DELETE');
                    }
                }
            }
        }

        // Enviar comando para IA gerar grade
        const prompt = turmaId 
            ? `Gere uma grade horária para a turma ${turmas.find(t => t.id == turmaId)?.nome}.`
            : 'Gere uma grade horária para todas as turmas da escola.';

        const response = await apiRequest('/chat/send', 'POST', {
            mensagem: prompt
        });

        if (response.success) {
            closeModal("generateGradeModal");
            showNotification('Grade horária gerada com sucesso!', 'success');
            // A grade será carregada automaticamente quando o usuário for redirecionado
        } else {
            throw new Error(response.error || 'Erro ao gerar grade');
        }
    } catch (error) {
        console.error('Erro ao gerar grade:', error);
        showNotification('Erro ao gerar grade horária', 'error');
    }
}

// Função para adicionar aula à grade
function addAulaGrade(turmaId, diaSemana, horario) {
    showNotification(`Adicionar aula para turma ${turmaId} em ${diaSemana} às ${horario}`, 'info');
    // TODO: Implementar modal para adicionar aula
}

// Função para editar aula da grade
function editAulaGrade(aulaId) {
    showNotification(`Editar aula ${aulaId}`, 'info');
    // TODO: Implementar modal para editar aula
}

// Função para limpar todas as grades horárias
async function clearAllGrades() {
    if (!confirm('Tem certeza que deseja limpar todas as grades horárias? Esta ação não pode ser desfeita.')) {
        return;
    }
    
    try {
        // Buscar todas as grades horárias
        const response = await apiRequest('/grade-horaria', 'GET');
        if (response.success && response.data.length > 0) {
            // Deletar todas as grades uma por uma
            let deletedCount = 0;
            for (const grade of response.data) {
                const deleteResponse = await apiRequest(`/grade-horaria/${grade.id}`, 'DELETE');
                if (deleteResponse.success) {
                    deletedCount++;
                }
            }
            
            if (deletedCount > 0) {
                showNotification(`✅ ${deletedCount} aulas removidas com sucesso!`, 'success');
                // Recarregar a grade horária (que agora estará vazia)
                await loadGradeHoraria();
            } else {
                showNotification('Nenhuma aula foi removida', 'warning');
            }
        } else {
            showNotification('Não há grades horárias para limpar', 'info');
        }
    } catch (error) {
        console.error('Erro ao limpar grades horárias:', error);
        showNotification('Erro ao limpar grades horárias', 'error');
    }
}

