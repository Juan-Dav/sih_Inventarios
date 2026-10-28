package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByEquipoIdAndEstado(Long equipoId, String estado);
    List<Prestamo> findByEstado(String estado);
}
