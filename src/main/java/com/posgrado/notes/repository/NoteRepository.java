package com.posgrado.notes.repository;

import com.posgrado.notes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // Todas las notas creadas por un profesor específico
    List<Note> findByProfesorId(Long profesorId);

    // Todas las notas de un estudiante específico
    List<Note> findByEstudianteId(Long estudianteId);

    /**
     * Busca una nota por id Y por profesorId en una sola consulta.
     * Si la nota existe pero pertenece a otro profesor, retorna Optional.empty()
     * → el servicio lanza AccessDeniedException → Spring retorna 403 automáticamente.
     * Este patrón evita una vulnerabilidad de Insecure Direct Object Reference (IDOR).
     */
    Optional<Note> findByIdAndProfesorId(Long id, Long profesorId);

    // Análogo para el estudiante al consultar su propia nota
    Optional<Note> findByIdAndEstudianteId(Long id, Long estudianteId);
}