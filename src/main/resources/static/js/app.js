
const API_BASE = '';
let currentUser = null;
let currentEditingNoteId = null;
let allUsers = [];


document.addEventListener('DOMContentLoaded', () => {
    attachEventListeners();
    loadApp();
});

function attachEventListeners() {
    document.getElementById('logout-btn').addEventListener('click', handleLogout);

    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.addEventListener('click', handleTabSwitch);
    });

    document.getElementById('note-form').addEventListener('submit', handleSaveNote);
    document.getElementById('cancel-form-btn').addEventListener('click', cancelEditNote);
    document.getElementById('create-note-tab').addEventListener('click', () => {
        resetNoteForm();
        switchTab('create-tab');
    });

    document.getElementById('refresh-notes-btn').addEventListener('click', loadNotes);

    document.querySelectorAll('.modal-close, .modal-close-btn').forEach(btn => {
        btn.addEventListener('click', closeModal);
    });

    document.getElementById('note-modal').addEventListener('click', (e) => {
        if (e.target === document.getElementById('note-modal')) closeModal();
    });
}

function handleLogout() {
    window.location.href = '/logout';
}


async function loadApp() {
    try {
        await loadCurrentUser();

        // Mostrar el contenedor solo cuando el usuario está cargado
        document.getElementById('app-container').style.display = 'flex';

        const notesTabBtn = document.getElementById('notes-tab-btn');
        const usersTabBtn = document.getElementById('users-tab-btn');
        const createNoteTabBtn = document.getElementById('create-note-tab');
        const usersSectionTitle = document.getElementById('users-section-title');
        const notesSectionTitle = document.getElementById('notes-section-title');

    
        if (currentUser.rol === 'PROFESOR') {
            notesTabBtn.textContent = 'Todas las Notas';
            notesSectionTitle.textContent = 'Todas las Notas';
            createNoteTabBtn.style.display = 'block';

            usersTabBtn.style.display = 'block';
            usersTabBtn.innerHTML = '👥 Estudiantes';
            usersSectionTitle.textContent = 'Estudiantes Registrados';

            await loadUsers('/students');

        } else if (currentUser.rol === 'ASISTENTE') {
            notesTabBtn.textContent = 'Todas las Notas';
            notesSectionTitle.textContent = 'Todas las Notas';
            createNoteTabBtn.style.display = 'none';

            usersTabBtn.style.display = 'block';
            usersTabBtn.innerHTML = '👥 Usuarios';
            usersSectionTitle.textContent = 'Usuarios Registrados';

            await loadUsers('/users');

        } else {
            // ESTUDIANTE
            notesTabBtn.textContent = 'Mis Notas';
            notesSectionTitle.textContent = 'Mis Notas';
            createNoteTabBtn.style.display = 'none';
            usersTabBtn.style.display = 'none';
        }

        await loadNotes();
        switchTab('notes-tab');
    } catch (error) {
        console.error('Error cargando app:', error);
        window.location.href = '/login';
    }
}

async function loadCurrentUser() {
    try {
        const response = await apiCall('/me');
        currentUser = response;
        document.getElementById('user-name').textContent =
            currentUser.nombreCompleto || currentUser.username || '';
        document.getElementById('user-role').textContent = currentUser.rol;
    } catch (error) {
        console.error('Error cargando usuario actual:', error);
        throw error;
    }
}


/**
 * Resuelve el endpoint correcto según el rol:
 *   ASISTENTE → GET /notes/all  (todas las notas del sistema)
 *   PROFESOR  → GET /notes      (solo las notas que él creó)
 *   ESTUDIANTE→ GET /notes      (solo las notas asignadas a él)
 */
function getNotesEndpoint() {
    if (currentUser.rol === 'ASISTENTE') return '/notes/all';
    return '/notes'; // PROFESOR y ESTUDIANTE comparten el endpoint; el backend filtra por rol
}

async function loadNotes() {
    try {
        const notesContainer = document.getElementById('notes-list');
        notesContainer.innerHTML = '<div class="loading">Cargando notas...</div>';

        const notes = await apiCall(getNotesEndpoint());

        if (!notes || notes.length === 0) {
            const emptyMessages = {
                PROFESOR:   'Aún no has creado ninguna nota. ¡Crea la primera!',
                ASISTENTE:  'No hay notas registradas en el sistema.',
                ESTUDIANTE: 'Aún no tienes notas asignadas.',
            };
            const emptyMessage = emptyMessages[currentUser.rol] || 'No hay notas disponibles.';

            notesContainer.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <h3>No hay notas disponibles</h3>
                    <p>${emptyMessage}</p>
                </div>
            `;
            return;
        }

        notesContainer.innerHTML = notes.map(note => `
            <div class="note-card" onclick="openNoteModal(${note.id})">
                <h3 class="note-title">${escapeHtml(note.titulo)}</h3>
                <p class="note-preview">${escapeHtml(note.descripcion)}</p>
                <div class="note-card-meta">
                    <span class="note-student">${escapeHtml(getStudentName(note.estudianteId))}</span>
                    ${note.calificacion !== null && note.calificacion !== undefined
                        ? `<span class="note-grade">${note.calificacion}/100</span>`
                        : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        document.getElementById('notes-list').innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <h3>Error al cargar las notas</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// ============================================
// CREAR / EDITAR NOTA (Solo Profesor)
// ============================================
async function handleSaveNote(e) {
    e.preventDefault();
    const title = document.getElementById('note-title').value.trim();
    const content = document.getElementById('note-content').value.trim();
    const studentId = document.getElementById('note-student').value;
    const grade = document.getElementById('note-grade').value;

    // Validación básica antes de enviar
    if (!title || !content || !studentId) {
        alert('Por favor, completa todos los campos obligatorios.');
        return;
    }

    try {
        const noteData = {
            titulo: title,
            descripcion: content,
            estudianteId: parseInt(studentId, 10),
            calificacion: grade !== '' ? parseFloat(grade) : null,
        };

        if (currentEditingNoteId) {
            await apiCall(`/notes/${currentEditingNoteId}`, {
                method: 'PUT',
                body: JSON.stringify(noteData),
            });
            alert('Nota actualizada correctamente');
        } else {
            await apiCall('/notes', {
                method: 'POST',
                body: JSON.stringify(noteData),
            });
            alert('Nota creada correctamente');
        }

        resetNoteForm();
        await loadNotes();
        switchTab('notes-tab');
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
}

function resetNoteForm() {
    document.getElementById('note-form').reset();
    document.getElementById('form-title').textContent = 'Crear Nueva Nota';
    currentEditingNoteId = null;
}

function cancelEditNote() {
    resetNoteForm();
    switchTab('notes-tab');
}

// ============================================
// MODAL DE NOTAS
// ============================================
async function openNoteModal(noteId) {
    try {
        const note = await apiCall(`/notes/${noteId}`);
        const modal = document.getElementById('note-modal');

        document.getElementById('modal-note-title').textContent = escapeHtml(note.titulo);
        document.getElementById('modal-note-content').textContent = escapeHtml(note.descripcion);
        document.getElementById('modal-note-student').textContent =
            escapeHtml(getStudentName(note.estudianteId));
        document.getElementById('modal-note-grade').textContent =
            note.calificacion !== null && note.calificacion !== undefined
                ? `${note.calificacion}/100`
                : 'No calificada';
        document.getElementById('modal-note-date').textContent = note.fechaCreacion
            ? new Date(note.fechaCreacion).toLocaleDateString('es-ES')
            : '-';

        const editBtn = document.getElementById('modal-edit-btn');
        const deleteBtn = document.getElementById('modal-delete-btn');

        // Solo el PROFESOR puede editar y eliminar
        if (currentUser.rol === 'PROFESOR') {
            editBtn.style.display = 'block';
            deleteBtn.style.display = 'block';
            editBtn.onclick = () => editNoteFromModal(noteId);
            deleteBtn.onclick = () => deleteNoteFromModal(noteId);
        } else {
            editBtn.style.display = 'none';
            deleteBtn.style.display = 'none';
        }

        // FIX: Solo añadir la clase 'show'; el CSS ya define display:flex en .modal.show
        modal.classList.add('show');
    } catch (error) {
        alert(`Error al abrir la nota: ${error.message}`);
    }
}

function closeModal() {
    const modal = document.getElementById('note-modal');
    // FIX: Quitar solo la clase; el CSS se encarga de ocultar el modal
    modal.classList.remove('show');
}

async function editNoteFromModal(noteId) {
    try {
        const note = await apiCall(`/notes/${noteId}`);
        document.getElementById('note-title').value = note.titulo;
        document.getElementById('note-content').value = note.descripcion;
        document.getElementById('note-student').value = note.estudianteId;
        document.getElementById('note-grade').value = note.calificacion ?? '';
        document.getElementById('form-title').textContent = 'Editar Nota';

        currentEditingNoteId = noteId;
        closeModal();
        switchTab('create-tab');
    } catch (error) {
        alert(`Error al cargar la nota para editar: ${error.message}`);
    }
}

async function deleteNoteFromModal(noteId) {
    if (!confirm('¿Estás seguro de que deseas eliminar esta nota?')) return;
    try {
        await apiCall(`/notes/${noteId}`, { method: 'DELETE' });
        alert('Nota eliminada correctamente');
        closeModal();
        await loadNotes();
    } catch (error) {
        alert(`Error al eliminar: ${error.message}`);
    }
}


async function loadUsers(endpoint) {
    try {
        allUsers = await apiCall(endpoint);

        // Llenar el select del formulario (solo para PROFESOR)
        if (currentUser.rol === 'PROFESOR') {
            const studentSelect = document.getElementById('note-student');
            studentSelect.innerHTML = '<option value="">Selecciona un estudiante</option>';
            allUsers.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.nombreCompleto || user.username || 'Estudiante';
                studentSelect.appendChild(option);
            });
        }

        displayUsersTable();
    } catch (error) {
        console.error('Error cargando lista de usuarios/estudiantes:', error);
        document.getElementById('users-list').innerHTML = `
            <div class="empty-state">
                <h3>Error al cargar usuarios</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

function displayUsersTable() {
    const usersContainer = document.getElementById('users-list');

    if (!allUsers || allUsers.length === 0) {
        usersContainer.innerHTML = `<div class="empty-state"><h3>No hay usuarios registrados</h3></div>`;
        return;
    }

    usersContainer.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Nombre completo</th>
                    <th>Email</th>
                    <th>Rol</th>
                </tr>
            </thead>
            <tbody>
                ${allUsers.map(user => `
                    <tr>
                        <td>${escapeHtml(user.nombreCompleto || user.username || 'Usuario')}</td>
                        <td>${escapeHtml(user.email || '-')}</td>
                        <td><span class="user-role">${escapeHtml(user.rol)}</span></td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

// ============================================
// NAVEGACIÓN DE TABS
// ============================================
function handleTabSwitch(e) {
    switchTab(e.currentTarget.getAttribute('data-tab'));
}

function switchTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    const tabElement = document.getElementById(tabName);
    if (tabElement) tabElement.classList.add('active');

    const buttonElement = document.querySelector(`[data-tab="${tabName}"]`);
    if (buttonElement) buttonElement.classList.add('active');
}

// ============================================
// UTILIDADES
// ============================================
function getStudentName(studentId) {
    // Manejo para ESTUDIANTE cuando allUsers está vacío
    if (currentUser && currentUser.rol === 'ESTUDIANTE') {
        if (studentId === currentUser.id) {
            return currentUser.nombreCompleto || currentUser.username || 'Yo';
        }
        return `Estudiante #${studentId}`;
    }
    const user = allUsers.find(s => s.id === studentId);
    return user
        ? (user.nombreCompleto || user.username || 'Desconocido')
        : `Estudiante #${studentId}`;
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    const stringText = String(text);
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return stringText.replace(/[&<>"']/g, m => map[m]);
}

// ============================================
// LLAMADAS A API
// ============================================
async function apiCall(endpoint, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
    };
    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: { ...defaultOptions.headers, ...(options.headers || {}) },
    };

    const response = await fetch(`${API_BASE}${endpoint}`, finalOptions);

    if (!response.ok) {
        if (response.status === 401) {
            window.location.href = '/login';
            throw new Error('No autenticado');
        }
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Error ${response.status}`);
    }

    if (response.status === 204) return null;
    return await response.json();
}