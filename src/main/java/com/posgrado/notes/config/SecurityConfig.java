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


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF deshabilitado para la demo REST — en producción re-evaluar
            .csrf(csrf -> csrf.disable())

            // Registramos nuestro proveedor de autenticación personalizado
            .authenticationProvider(authenticationProvider())

            .authorizeHttpRequests(auth -> auth
                // 1. RUTAS PÚBLICAS: Libre acceso sin credenciales
                .requestMatchers("/login", "/login.html", "/error").permitAll()

                // 2. ROL ASISTENTE: Alcance global de auditoría (Lectura total)
                .requestMatchers(HttpMethod.GET, "/notes/all").hasRole("ASISTENTE")
                .requestMatchers(HttpMethod.GET, "/users").hasRole("ASISTENTE")

                // 3. ROL PROFESOR: Acciones de escritura y gestión sobre las notas y alumnos
                .requestMatchers(HttpMethod.POST,   "/notes").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.PUT,    "/notes/**").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.DELETE, "/notes/**").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.GET,    "/students").hasRole("PROFESOR")

                // 4. ROL ESTUDIANTE (Y PROFESOR): Consulta de calificaciones
                .requestMatchers(HttpMethod.GET, "/notes").hasAnyRole("PROFESOR", "ESTUDIANTE")
                .requestMatchers(HttpMethod.GET, "/notes/*").hasAnyRole("PROFESOR", "ESTUDIANTE")

                // 5. CUALQUIER ROL AUTENTICADO: Autogestión de perfil corporativo
                .requestMatchers(HttpMethod.GET, "/me").authenticated()

                // 6. CIERRE PERIMETRAL: Cualquier otra ruta no declarada explícitamente requiere inicio de sesión
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", true)   // tras login exitoso → info del usuario
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .sessionManagement(session -> session
                // A dónde redirigir si la sesión ya no es válida (por inactividad)
                .invalidSessionUrl("/login.html") 
                
                // Opcional: Control de concurrencia (evita que el mismo usuario inicie sesión en 2 navegadores distintos a la vez)
                .maximumSessions(1) 
                .expiredUrl("/login.html")
            )

            .exceptionHandling(ex -> ex
                // 1. Maneja el error 403 (Tiene sesión, pero no tiene el rol adecuado)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/error?status=403");
                })
                // 2. Maneja el error 401 (No tiene sesión) con redirección inteligente
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    
                    // Si el usuario sin sesión intenta entrar a la raíz o a un recurso visual...
                    if (uri.equals("/") || uri.endsWith(".html")) {
                        response.sendRedirect("/login.html");
                    } else {
                        // Si es una petición asíncrona o API REST (ej: /notes), mandamos el error para que lo maneje tu JS
                        response.sendRedirect("/error?status=401");
                    }
                })
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)   // destruye la HttpSession en el servidor
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

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
