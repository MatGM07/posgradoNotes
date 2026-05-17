package com.posgrado.notes.service;

import com.posgrado.notes.model.Note;
import com.posgrado.notes.model.Rol;
import com.posgrado.notes.model.UserInfo;
import com.posgrado.notes.repository.NoteRepository;
import com.posgrado.notes.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Capa de negocio donde se implementan las reglas de acceso por rol.
 *
 * DISEÑO INTENCIONAL: las reglas "solo mis notas" se aplican AQUÍ
 * (en la capa de servicio), complementando la seguridad por URL
 * configurada en SecurityConfig. Esto demuestra defensa en profundidad:
 * - SecurityConfig protege por tipo de operación (HTTP method / URL)
 * - NoteService protege por pertenencia de datos (¿es MI nota?)
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserInfoRepository userInfoRepository;

    /**
     * PUNTO CLAVE — SecurityContextHolder
     *
     * Después del login exitoso, Spring Security almacena el contexto
     * de autenticación en el SecurityContextHolder (asociado al hilo HTTP).
     * Este método lo usa para obtener el usuario actual sin necesidad de
     * recibirlo como parámetro en cada request → principio de transparencia.
     */
    private UserInfo getUsuarioAutenticado() {
        // Extraemos el username del contexto de seguridad del hilo actual
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario autenticado no encontrado en BD: " + username));
    }

    // Método público para el endpoint /me
    public UserInfo obtenerUsuarioActual() {
        return getUsuarioAutenticado();
    }

    /**
     * GET /notes — cada rol ve solo sus notas.
     * La diferenciación es por rol, aplicada en la capa de servicio.
     */
    public List<Note> listarMisNotas() {
        UserInfo usuario = getUsuarioAutenticado();

        // REGLA DE NEGOCIO: filtro por rol — ningún rol ve notas ajenas
        if (usuario.getRol() == Rol.PROFESOR) {
            return noteRepository.findByProfesorId(usuario.getId());
        } else {
            // ESTUDIANTE solo ve las notas donde él es el destinatario
            return noteRepository.findByEstudianteId(usuario.getId());
        }
    }

    /**
     * GET /notes/{id} — validación de pertenencia según rol.
     * Patrón anti-IDOR: la query incluye el id del usuario como filtro adicional.
     */
    public Note buscarPorId(Long id) {
        UserInfo usuario = getUsuarioAutenticado();

        if (usuario.getRol() == Rol.PROFESOR) {
            // El PROFESOR solo puede ver una nota si él la creó
            return noteRepository.findByIdAndProfesorId(id, usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No tienes permiso para ver esta nota"));
        } else {
            // El ESTUDIANTE solo puede ver una nota si él es el destinatario
            return noteRepository.findByIdAndEstudianteId(id, usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No tienes permiso para ver esta nota"));
        }
    }

    /**
     * POST /notes — solo PROFESOR (la URL ya está protegida en SecurityConfig).
     * El profesorId se toma del contexto de seguridad, no del request body,
     * evitando que el profesor se impersone a otro.
     */
    public Note crear(Note note) {
        UserInfo profesor = getUsuarioAutenticado();

        // Verificamos que el estudiante destinatario existe en la BD
        userInfoRepository.findById(note.getEstudianteId())
                .filter(u -> u.getRol() == Rol.ESTUDIANTE)
                .orElseThrow(() -> new RuntimeException(
                        "Estudiante no encontrado con id: " + note.getEstudianteId()));

        // SEGURIDAD: asignamos el profesorId desde el contexto autenticado
        // El cliente no puede enviar un profesorId diferente en el body
        note.setProfesorId(profesor.getId());

        return noteRepository.save(note);
    }

    /**
     * PUT /notes/{id} — solo PROFESOR, y solo sus propias notas.
     * La query findByIdAndProfesorId garantiza que el update falle
     * si la nota pertenece a otro profesor (HTTP 403).
     */
    public Note actualizar(Long id, Note datosNuevos) {
        UserInfo profesor = getUsuarioAutenticado();

        // REGLA DE SEGURIDAD: la búsqueda incluye el id del profesor autenticado
        Note nota = noteRepository.findByIdAndProfesorId(id, profesor.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No tienes permiso para editar esta nota"));

        // Actualizamos solo los campos modificables por el profesor
        nota.setTitulo(datosNuevos.getTitulo());
        nota.setDescripcion(datosNuevos.getDescripcion());
        nota.setCalificacion(datosNuevos.getCalificacion());
        nota.setEstudianteId(datosNuevos.getEstudianteId());
        // fechaCreacion y profesorId son inmutables

        return noteRepository.save(nota);
    }

    /**
     * DELETE /notes/{id} — solo PROFESOR, y solo sus propias notas.
     * Mismo patrón anti-IDOR que en actualizar().
     */
    public void eliminar(Long id) {
        UserInfo profesor = getUsuarioAutenticado();

        Note nota = noteRepository.findByIdAndProfesorId(id, profesor.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No tienes permiso para eliminar esta nota"));

        noteRepository.delete(nota);
    }

    /**
     * GET /students — solo PROFESOR: lista los estudiantes disponibles
     * para poder seleccionarlos al crear una nota.
     */
    public List<UserInfo> listarEstudiantes() {
        return userInfoRepository.findByRol(Rol.ESTUDIANTE);
    }
}