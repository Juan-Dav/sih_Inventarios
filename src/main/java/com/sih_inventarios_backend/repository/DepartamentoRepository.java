package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
    List<Departamento> findByAreaId(Long areaId);
}
