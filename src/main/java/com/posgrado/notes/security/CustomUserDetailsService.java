package com.posgrado.notes.security;

import com.posgrado.notes.model.UserInfo;
import com.posgrado.notes.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║          PIEZA CENTRAL DE LA ARQUITECTURA DE SEGURIDAD          ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║ Este UserDetailsService es el puente entre la tabla 'users'     ║
 * ║ (gestionada por admin-service) y el mecanismo de autenticación  ║
 * ║ de Spring Security en notes-service.                            ║
 * ║                                                                 ║
 * ║ Spring Security llama a loadUserByUsername() automáticamente    ║
 * ║ cuando el usuario envía sus credenciales al endpoint /login.    ║
 * ║ Luego compara la contraseña ingresada con el hash BCrypt de BD. ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInfoRepository userInfoRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscamos el usuario en la tabla 'users' del admin-service
        UserInfo userInfo = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no registrado en el sistema: " + username));

        // 2. Convertimos el rol del enum al formato de autoridad de Spring Security.
        //    Spring Security requiere el prefijo "ROLE_" para que .hasRole("PROFESOR")
        //    funcione. Ejemplo: Rol.PROFESOR → "ROLE_PROFESOR"
        String authority = "ROLE_" + userInfo.getRol().name();

        // 3. Construimos el UserDetails que Spring Security usará para:
        //    a) Verificar la contraseña (BCrypt compare)
        //    b) Cargar las autoridades en el SecurityContext
        //    c) Determinar qué endpoints puede acceder este usuario
        return new User(
                userInfo.getUsername(),
                userInfo.getPassword(), // hash BCrypt — Spring lo compara internamente
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}