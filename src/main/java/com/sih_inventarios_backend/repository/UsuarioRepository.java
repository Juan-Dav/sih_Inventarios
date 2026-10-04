package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Este método lo usaremos más adelante para el Login
    Optional<Usuario> findByCorreo(String correo);

    /** Cuentas de un rol; lo usa el arranque para saber si ya hay un administrador. */
    Optional<Usuario> findFirstByRolNombre(String nombreRol);
}