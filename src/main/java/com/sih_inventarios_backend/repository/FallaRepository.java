package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Falla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FallaRepository extends JpaRepository<Falla, Long> {

    /** Fallas de un equipo: el historial de esa máquina. */
    List<Falla> findByEquipoIdOrderByFechaReporteDesc(Long equipoId);

    /** Cuántas fallas hay en un estado concreto (pendientes, en revisión, resueltas). */
    long countByEstado(String estado);

    /** Fallas de un estado concreto, de la más reciente a la más antigua. */
    List<Falla> findByEstadoOrderByFechaReporteDesc(String estado);

    /** Cola de trabajo del técnico: todo lo que todavía no está resuelto. */
    List<Falla> findByEstadoNotOrderByFechaReporteAsc(String estado);
}
