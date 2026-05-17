package com.posgrado.notes.model;

/**
 * Mismo enum que admin-service — debe tener exactamente los mismos valores
 * para que el mapeo @Enumerated(EnumType.STRING) sea consistente en ambos apps.
 */
public enum Rol {
    PROFESOR,
    ESTUDIANTE
}