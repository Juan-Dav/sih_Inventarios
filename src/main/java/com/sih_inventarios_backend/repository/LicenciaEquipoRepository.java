package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.LicenciaEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LicenciaEquipoRepository extends JpaRepository<LicenciaEquipo, Long> {
    List<LicenciaEquipo> findByLicenciaId(Long licenciaId);
    List<LicenciaEquipo> findByEquipoId(Long equipoId);
}
