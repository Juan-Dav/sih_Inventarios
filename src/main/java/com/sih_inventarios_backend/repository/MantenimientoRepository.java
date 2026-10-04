package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    /** Mantenimientos de un equipo: el historial de esa máquina. */
    List<Mantenimiento> findByEquipoIdOrderByFechaMantenimientoDesc(Long equipoId);

    /** Todo el historial, del más reciente al más antiguo. */
    List<Mantenimiento> findAllByOrderByFechaMantenimientoDesc();
}
