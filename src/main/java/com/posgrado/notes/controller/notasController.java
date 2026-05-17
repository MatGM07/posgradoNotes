package com.posgrado.notes.controller;

import com.posgrado.notes.model.Note;
import com.posgrado.notes.model.UserInfo;
import com.posgrado.notes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador único que expone todos los endpoints del notes-service.
 * Las rutas están protegidas en dos niveles:
 *   1. Por URL/método en SecurityConfig  → quién puede llamar al endpoint
 *   2. Por lógica en NoteService         → si el recurso le pertenece al usuario
 */
@RestController
@RequiredArgsConstructor
public class notasController {

    private final NoteService noteService;

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINTS DE NOTAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /notes
     * PROFESOR → retorna las notas que él creó
     * ESTUDIANTE → retorna las notas asignadas a él
     * Ambos roles tienen acceso, pero ven datos diferentes (filtro en servicio).
     */
    @GetMapping("/notes")
    public ResponseEntity<List<Note>> listar() {
        return ResponseEntity.ok(noteService.listarMisNotas());
    }

    /**
     * GET /notes/{id}
     * Acceso para ambos roles, con validación de pertenencia en el servicio.
     * Si el recurso no pertenece al usuario autenticado → 403 Forbidden.
     */
    @GetMapping("/notes/{id}")
    public ResponseEntity<Note> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.buscarPorId(id));
    }

    /**
     * POST /notes — SOLO PROFESOR (bloqueado en SecurityConfig para ESTUDIANTE)
     * El profesorId se asigna automáticamente desde el contexto de seguridad.
     */
    @PostMapping("/notes")
    public ResponseEntity<Note> crear(@RequestBody Note note) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.crear(note));
    }

    /**
     * PUT /notes/{id} — SOLO PROFESOR (bloqueado en SecurityConfig)
     * Adicionalmente, en NoteService se verifica que la nota le pertenezca.
     */
    @PutMapping("/notes/{id}")
    public ResponseEntity<Note> actualizar(@PathVariable Long id,
                                           @RequestBody Note note) {
        return ResponseEntity.ok(noteService.actualizar(id, note));
    }

    /**
     * DELETE /notes/{id} — SOLO PROFESOR (bloqueado en SecurityConfig)
     * Adicionalmente, en NoteService se verifica que la nota le pertenezca.
     */
    @DeleteMapping("/notes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        noteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINTS AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /students — SOLO PROFESOR
     * Lista los estudiantes registrados en el sistema para que el profesor
     * pueda seleccionar a quién asignarle una nota.
     */
    @GetMapping("/students")
    public ResponseEntity<List<UserInfo>> listarEstudiantes() {
        return ResponseEntity.ok(noteService.listarEstudiantes());
    }

    /**
     * GET /me — cualquier usuario autenticado
     * Retorna la información básica del usuario actualmente autenticado.
     * Útil para que el cliente frontend conozca el rol y el id del usuario.
     * La contraseña NO se incluye gracias a @JsonProperty(WRITE_ONLY) en UserInfo.
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> me() {
        return ResponseEntity.ok(noteService.obtenerUsuarioActual());
    }
}
