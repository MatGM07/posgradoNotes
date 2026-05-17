package com.posgrado.notes.repository;

import com.posgrado.notes.model.Rol;
import com.posgrado.notes.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    // Usado por CustomUserDetailsService durante el login
    Optional<UserInfo> findByUsername(String username);

    // Usado por el PROFESOR para listar estudiantes a los que asignar notas
    List<UserInfo> findByRol(Rol rol);
}