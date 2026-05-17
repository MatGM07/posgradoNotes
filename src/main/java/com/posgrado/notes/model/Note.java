package com.posgrado.notes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad central del negocio.
 * Vincula estudiante y profesor mediante IDs (sin @ManyToOne explícito)
 * para mantener el desacoplamiento entre los dos aplicativos.
 * Las validaciones de existencia y pertenencia se hacen en NoteService.
 */
@Entity
@Table(name = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Double calificacion;

    // Referencia al id del estudiante en la tabla users (no hay FK explícita por diseño)
    @Column(nullable = false)
    private Long estudianteId;

    // Referencia al id del profesor creador — clave para las reglas de negocio
    @Column(nullable = false)
    private Long profesorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Asigna automáticamente la fecha de creación antes del primer INSERT.
     * El profesor no puede manipular este campo desde el request body.
     */
    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
