package com.posgrado.notes.config;

import com.posgrado.notes.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           CONFIGURACIÓN DE SEGURIDAD — notes-service            ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║ Contrasta con admin-service en dos aspectos clave:              ║
 * ║ 1. Usa DaoAuthenticationProvider + CustomUserDetailsService     ║
 * ║    (BD) en lugar de InMemoryUserDetailsManager.                 ║
 * ║ 2. Tiene reglas de autorización por ROL Y por método HTTP,      ║
 * ║    reflejando un modelo de negocio real con dos tipos de usuario.║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    /**
     * PUNTO CLAVE 1 — SecurityFilterChain con reglas por rol
     *
     * Se definen dos niveles de protección:
     *   a) Por método HTTP: POST/PUT/DELETE en /notes solo para PROFESOR
     *   b) Por URL: /students solo para PROFESOR
     *   c) Todo lo demás requiere autenticación (cualquier rol)
     *
     * El ESTUDIANTE intentar llamar POST /notes → 403 Forbidden
     * sin llegar siquiera al controlador.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF deshabilitado para la demo REST — en producción re-evaluar
            .csrf(csrf -> csrf.disable())

            // Registramos nuestro proveedor de autenticación personalizado
            .authenticationProvider(authenticationProvider())

            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/login", "/error").permitAll()

                // Solo PROFESOR puede crear, modificar y eliminar notas
                .requestMatchers(HttpMethod.POST,   "/notes").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.PUT,    "/notes/**").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.DELETE, "/notes/**").hasRole("PROFESOR")

                // Solo PROFESOR puede consultar el listado de estudiantes
                .requestMatchers(HttpMethod.GET, "/students").hasRole("PROFESOR")

                // Leer notas y /me: cualquier usuario autenticado (ambos roles)
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", true)   // tras login exitoso → info del usuario
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)   // destruye la HttpSession en el servidor
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * PUNTO CLAVE 2 — DaoAuthenticationProvider
     *
     * Conecta Spring Security con nuestra fuente de datos personalizada.
     * Al configurar explícitamente este provider:
     *   - Spring sabe que debe usar CustomUserDetailsService para cargar usuarios
     *   - Spring sabe que las contraseñas están hasheadas con BCrypt
     *   - El proceso de login es: cargar usuario → verificar hash → crear sesión
     *
     * Esto contrasta con admin-service donde el provider se configura
     * automáticamente al declarar el bean InMemoryUserDetailsManager.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService); // fuente: tabla users en MySQL
        provider.setPasswordEncoder(passwordEncoder());           // algoritmo: BCrypt
        return provider;
    }

    /**
     * PUNTO CLAVE 3 — BCryptPasswordEncoder compartido
     *
     * El mismo algoritmo que usó admin-service para encriptar las contraseñas.
     * Ambas aplicaciones deben coincidir en el encoder, de lo contrario
     * la verificación de contraseñas fallará aunque los datos sean correctos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
