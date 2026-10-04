package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Rol; // <-- Agregamos esta importación
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> { // <-- Le quitamos el <Rol> a RolRepository
    // Este método nos permitirá buscar un rol por su nombre (Ej: "ADMINISTRADOR")
    Rol findByNombre(String nombre);
}