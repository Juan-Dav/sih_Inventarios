package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    /** Equipos de una sala concreta: el conteo por aula sale de aquí. */
    List<Equipo> findBySalaId(Long salaId);

    /**
     * Todos los equipos con su sala ya resuelta en la misma consulta.
     *
     * El módulo de verificación cuenta los equipos de todas las aulas a la vez;
     * sin el JOIN FETCH, cargar la lista dispararía una consulta extra por cada
     * equipo para traer su sala (N+1).
     */
    @Query("select e from Equipo e join fetch e.sala order by e.codigo")
    List<Equipo> findAllConSala();

    /** Cuántos equipos hay registrados en una sala, sin traerlos todos. */
    long countBySalaId(Long salaId);

    /** Cuántos equipos hay en un estado concreto en toda la institución. */
    long countByEstado(String estado);

    /** Equipos filtrados por estado, ordenados por código para la tabla. */
    List<Equipo> findByEstadoOrderByCodigoAsc(String estado);

    /** Cuántos equipos de una sala están en un estado concreto. */
    long countBySalaIdAndEstado(Long salaId, String estado);

    /** El más reciente de una sala, para saber cuándo se hizo el último conteo. */
    List<Equipo> findBySalaIdOrderByIdDesc(Long salaId);
}
