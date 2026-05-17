// ============================================
// GESTIÓN DE AUTENTICACIÓN Y ESTADO GLOBAL
// ============================================

const API_BASE = '';
let currentUser = null;
let currentEditingNoteId = null;
let allStudents = [];

// ============================================
// INICIALIZACIÓN
// ============================================

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

    // Modal — los botones edit/delete NO se registran aquí porque reciben el noteId
    // dinámicamente via onclick dentro de openNoteModal()
    document.querySelector('.modal-close').addEventListener('click', closeModal);
    document.querySelector('.modal-close-btn').addEventListener('click', closeModal);

    // Cerrar modal al hacer clic fuera
    document.getElementById('note-modal').addEventListener('click', (e) => {
        if (e.target === document.getElementById('note-modal')) {
            closeModal();
        }
    });
}

// ============================================
// SESSION / LOGOUT
// ============================================

function handleLogout() {
    window.location.href = '/logout';
}

// ============================================
// CARGAR APLICACIÓN
// ============================================

async function loadApp() {
    try {
        document.getElementById('app-container').style.display = 'flex';

        // Cargar usuario actual
        await loadCurrentUser();

        // Cargar estudiantes si es profesor
        if (currentUser.rol === 'PROFESOR') {
            await loadStudents();
            document.getElementById('create-note-tab').style.display = 'block';
            document.getElementById('students-tab-btn').style.display = 'block';
        }

        // Cargar notas
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

        // Actualizar UI
        document.getElementById('user-name').textContent = currentUser.nombreCompleto || currentUser.username || '';
        document.getElementById('user-role').textContent = currentUser.rol;
    } catch (error) {
        console.error('Error cargando usuario actual:', error);
        throw error;
    }
}

// ============================================
// CARGAR NOTAS
// ============================================

async function loadNotes() {
    try {
        const notesContainer = document.getElementById('notes-list');
        notesContainer.innerHTML = '<div class="loading">Cargando notas...</div>';

        const notes = await apiCall('/notes');

        if (notes.length === 0) {
            notesContainer.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <h3>No hay notas disponibles</h3>
                    <p>${currentUser.rol === 'PROFESOR' ? 'Crea tu primera nota' : 'Aún no tienes notas asignadas'}</p>
                </div>
            `;
            return;
        }

        notesContainer.innerHTML = notes.map(note => `
            <div class="note-card" onclick="openNoteModal(${note.id})">
                <h3 class="note-title">${escapeHtml(note.titulo)}</h3>
                <p class="note-preview">${escapeHtml(note.descripcion)}</p>
                <div class="note-meta">
                    <span class="note-student">${escapeHtml(getStudentName(note.estudianteId))}</span>
                    ${note.calificacion !== null && note.calificacion !== undefined ? `<span class="note-grade">${note.calificacion}/100</span>` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error cargando notas:', error);
        document.getElementById('notes-list').innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <h3>Error al cargar las notas</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// ============================================
// CREAR/EDITAR NOTA
// ============================================

async function handleSaveNote(e) {
    e.preventDefault();

    const title = document.getElementById('note-title').value;
    const content = document.getElementById('note-content').value;
    const studentId = document.getElementById('note-student').value;
    const grade = document.getElementById('note-grade').value;

    try {
        const noteData = {
            titulo: title,
            descripcion: content,
            estudianteId: parseInt(studentId),
            calificacion: grade ? parseFloat(grade) : 0,
        };

        if (currentEditingNoteId) {
            // Editar
            await apiCall(`/notes/${currentEditingNoteId}`, {
                method: 'PUT',
                body: JSON.stringify(noteData),
            });
            alert('Nota actualizada correctamente');
        } else {
            // Crear
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
        document.getElementById('modal-note-student').textContent = escapeHtml(getStudentName(note.estudianteId));
        document.getElementById('modal-note-grade').textContent = note.calificacion !== null && note.calificacion !== undefined ? `${note.calificacion}/100` : 'No calificada';
        document.getElementById('modal-note-date').textContent = new Date(note.fechaCreacion).toLocaleDateString('es-ES');

        // Mostrar botones solo para el profesor
        const editBtn = document.getElementById('modal-edit-btn');
        const deleteBtn = document.getElementById('modal-delete-btn');

        if (currentUser.rol === 'PROFESOR') {
            editBtn.style.display = 'block';
            deleteBtn.style.display = 'block';
            editBtn.onclick = () => editNoteFromModal(noteId);
            deleteBtn.onclick = () => deleteNoteFromModal(noteId);
        } else {
            editBtn.style.display = 'none';
            deleteBtn.style.display = 'none';
        }

        modal.style.display = 'flex';
        modal.classList.add('show');
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
}

function closeModal() {
    const modal = document.getElementById('note-modal');
    modal.classList.remove('show');
    modal.style.display = 'none';
}

async function editNoteFromModal(noteId) {
    try {
        const note = await apiCall(`/notes/${noteId}`);

        document.getElementById('note-title').value = note.titulo;
        document.getElementById('note-content').value = note.descripcion;
        document.getElementById('note-student').value = note.estudianteId;
        document.getElementById('note-grade').value = note.calificacion || '';
        document.getElementById('form-title').textContent = 'Editar Nota';

        currentEditingNoteId = noteId;
        closeModal();
        switchTab('create-tab');
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
}

async function deleteNoteFromModal(noteId) {
    if (!confirm('¿Estás seguro de que deseas eliminar esta nota?')) {
        return;
    }

    try {
        await apiCall(`/notes/${noteId}`, {
            method: 'DELETE',
        });
        alert('Nota eliminada correctamente');
        closeModal();
        await loadNotes();
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
}

// ============================================
// CARGAR ESTUDIANTES
// ============================================

async function loadStudents() {
    try {
        allStudents = await apiCall('/students');
        console.log(allStudents);
        // Actualizar select de estudiantes en el formulario
        const studentSelect = document.getElementById('note-student');
        studentSelect.innerHTML = '<option value="">Selecciona un estudiante</option>';

        allStudents.forEach(student => {
            const option = document.createElement('option');
            option.value = student.id;
            option.textContent = student.nombreCompleto || student.username || 'Estudiante';
            studentSelect.appendChild(option);
        });

        // Mostrar tabla de estudiantes
        displayStudentsTable();
    } catch (error) {
        console.error('Error cargando estudiantes:', error);
    }
}

function displayStudentsTable() {
    const studentsContainer = document.getElementById('students-list');

    if (allStudents.length === 0) {
        studentsContainer.innerHTML = `
            <div class="empty-state">
                <h3>No hay estudiantes registrados</h3>
            </div>
        `;
        return;
    }

    const html = `
        <table>
            <thead>
                <tr>
                    <th>Nombre completo</th>
                    <th>Email</th>
                    <th>Rol</th>
                </tr>
            </thead>
            <tbody>
                ${allStudents.map(student => `
                    <tr>
                        <td>${escapeHtml(student.nombreCompleto || student.username || 'Estudiante')}</td>
                        <td>${escapeHtml(student.email)}</td>
                        <td><span class="user-role">${student.rol}</span></td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    studentsContainer.innerHTML = html;
}

// ============================================
// NAVEGACIÓN DE TABS
// ============================================

function handleTabSwitch(e) {
    const tabName = e.target.getAttribute('data-tab');
    switchTab(tabName);
}

function switchTab(tabName) {
    // Ocultar todos los tabs (sobreescribiendo el inline style del HTML)
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
        tab.style.display = 'none';
    });

    // Desactivar todos los botones
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    // Mostrar el tab seleccionado
    const tabElement = document.getElementById(tabName);
    if (tabElement) {
        tabElement.classList.add('active');
        tabElement.style.display = 'block';
    }

    // Activar el botón correspondiente
    const buttonElement = document.querySelector(`[data-tab="${tabName}"]`);
    if (buttonElement) {
        buttonElement.classList.add('active');
    }
}

// ============================================
// UTILIDADES
// ============================================

function getStudentName(studentId) {
    const student = allStudents.find(s => s.id === studentId);
    return student ? (student.nombreCompleto || student.username || 'Desconocido') : 'Desconocido';
}

function escapeHtml(text) {
    // Si viene null o undefined, devolvemos un texto vacío de inmediato sin romper la app
    if (text === null || text === undefined) return '';
    
    const stringText = String(text);
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return stringText.replace(/[&<>"']/g, m => map[m]);
}

// ============================================
// LLAMADAS A API
// ============================================

async function apiCall(endpoint, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'same-origin',
    };

    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers,
        },
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

    // Para DELETE, puede no haber contenido
    if (response.status === 204) {
        return null;
    }

    return await response.json();
}
