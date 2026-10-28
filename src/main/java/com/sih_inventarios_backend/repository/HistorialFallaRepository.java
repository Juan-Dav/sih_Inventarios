package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.HistorialFalla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialFallaRepository extends JpaRepository<HistorialFalla, Long> {
    List<HistorialFalla> findByFallaIdOrderByFechaCambioDesc(Long fallaId);
}
