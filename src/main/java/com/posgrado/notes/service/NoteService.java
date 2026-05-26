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

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserInfoRepository userInfoRepository;

    private UserInfo getUsuarioAutenticado() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario autenticado no encontrado en BD: " + username));
    }

    public UserInfo obtenerUsuarioActual() {
        return getUsuarioAutenticado();
    }


    public List<Note> listarMisNotas() {
        UserInfo usuario = getUsuarioAutenticado();

        if (usuario.getRol() == Rol.PROFESOR) {
            return noteRepository.findByProfesorId(usuario.getId());
        } else if (usuario.getRol() == Rol.ESTUDIANTE) {
            return noteRepository.findByEstudianteId(usuario.getId());
        } else {
            // Un ASISTENTE no tiene "sus propias notas", si llama a este endpoint
            // por error, le denegamos el acceso o le devolvemos una lista vacía.
            throw new AccessDeniedException("Los asistentes deben usar la ruta global de notas.");
        }
    }


    public Note buscarPorId(Long id) {
        UserInfo usuario = getUsuarioAutenticado();

        if (usuario.getRol() == Rol.ASISTENTE) {
            // El ASISTENTE tiene acceso global, usamos el findById normal de JPA
            return noteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
        } else if (usuario.getRol() == Rol.PROFESOR) {
            return noteRepository.findByIdAndProfesorId(id, usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No tienes permiso para ver esta nota"));
        } else {
            return noteRepository.findByIdAndEstudianteId(id, usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No tienes permiso para ver esta nota"));
        }
    }

    public Note crear(Note note) {
        UserInfo profesor = getUsuarioAutenticado();

        userInfoRepository.findById(note.getEstudianteId())
                .filter(u -> u.getRol() == Rol.ESTUDIANTE)
                .orElseThrow(() -> new RuntimeException(
                        "Estudiante no encontrado con id: " + note.getEstudianteId()));

        note.setProfesorId(profesor.getId());
        return noteRepository.save(note);
    }

    public Note actualizar(Long id, Note datosNuevos) {
        UserInfo profesor = getUsuarioAutenticado();

        Note nota = noteRepository.findByIdAndProfesorId(id, profesor.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No tienes permiso para editar esta nota"));

        nota.setTitulo(datosNuevos.getTitulo());
        nota.setDescripcion(datosNuevos.getDescripcion());
        nota.setCalificacion(datosNuevos.getCalificacion());
        nota.setEstudianteId(datosNuevos.getEstudianteId());

        return noteRepository.save(nota);
    }

    public void eliminar(Long id) {
        UserInfo profesor = getUsuarioAutenticado();

        Note nota = noteRepository.findByIdAndProfesorId(id, profesor.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No tienes permiso para eliminar esta nota"));

        noteRepository.delete(nota);
    }

    public List<UserInfo> listarEstudiantes() {
        return userInfoRepository.findByRol(Rol.ESTUDIANTE);
    }


    public List<Note> listarTodasLasNotas() {
        // findAll() ya viene heredado de JpaRepository
        return noteRepository.findAll();
    }

    public List<UserInfo> listarTodosLosUsuarios() {
        // findAll() ya viene heredado de JpaRepository
        return userInfoRepository.findAll();
    }
}