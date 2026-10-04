package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Verificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VerificacionRepository extends JpaRepository<Verificacion, Long> {

    /** Verificaciones de una sala, de la más reciente a la más antigua. */
    List<Verificacion> findBySalaIdOrderByFechaVerificacionDesc(Long salaId);

    /**
     * Conteos de una sala con la sala y el técnico resueltos en la consulta.
     *
     * Las relaciones son EAGER, pero sin JOIN FETCH Hibernate las carga con una
     * consulta adicional por cada fila (N+1).
     */
    @Query("""
            select v from Verificacion v
            join fetch v.sala
            left join fetch v.tecnico
            where v.sala.id = :salaId
            order by v.fechaVerificacion desc
            """)
    List<Verificacion> findBySalaIdConDetalles(@Param("salaId") Long salaId);

    /** Historial completo de conteos, para el módulo del técnico. */
    List<Verificacion> findAllByOrderByFechaVerificacionDesc();

    /**
     * Historial completo con la sala y el técnico ya resueltos.
     *
     * Es la versión que usa el módulo de verificación: al traer todas las aulas
     * y todos los conteos, el N+1 de las relaciones EAGER se multiplica y este
     * endpoint se vuelve lento.
     */
    @Query("""
            select v from Verificacion v
            join fetch v.sala
            left join fetch v.tecnico
            order by v.fechaVerificacion desc
            """)
    List<Verificacion> findAllConDetalles();

    /** Cuántas veces se ha recorrido una sala. */
    long countBySalaId(Long salaId);
}
