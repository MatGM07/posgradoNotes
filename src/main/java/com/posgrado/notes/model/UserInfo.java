package com.posgrado.notes.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad de SOLO LECTURA que mapea la tabla 'users' creada por admin-service.
 *
 * CONCEPTO CLAVE: notes-service NO gestiona esta tabla.
 * Solo la lee para autenticar usuarios y para obtener información básica
 * (nombre, rol, id) al momento de asignar notas.
 *
 * Al no tener @OneToMany ni cascadas, JPA no intentará modificar registros
 * de usuarios desde este aplicativo.
 */
@Entity
@Table(name = "users")  // misma tabla que la entidad User del admin-service
@Data
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    /**
     * La contraseña se lee de BD (hash BCrypt) para que Spring Security
     * pueda verificarla durante el login, pero NUNCA se expone en respuestas JSON.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    private String nombreCompleto;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;
}