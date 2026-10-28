package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.HistorialCustodia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialCustodiaRepository extends JpaRepository<HistorialCustodia, Long> {
    List<HistorialCustodia> findByEquipoIdOrderByFechaCambioDesc(Long equipoId);
}
