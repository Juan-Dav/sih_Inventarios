package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    /**
     * Primera fila del plan, que es la única que importa.
     *
     * Se ordena por id para que el resultado sea estable: la tabla debería
     * tener siempre una sola fila, y así no depende de eso.
     */
    Optional<Plan> findFirstByOrderByIdAsc();
}