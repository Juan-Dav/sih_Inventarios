package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.dto.PlanResponse;
import com.sih_inventarios_backend.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Plan de la institución y compra.
 *
 * El plan pertenece al sistema, no a un usuario: lo consultan los tres paneles
 * y lo cambian los tres. Guardarlo en una sola fila y no por cuenta es a
 * propósito: los límites de inventario son de la institución, así que tiene que
 * ser el mismo número para el técnico, el docente y la administración.
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /** Estado actual del plan junto con el consumo. */
    @GetMapping
    public PlanResponse obtenerPlan() {
        return planService.obtenerEstado();
    }

    /** Compra simulada del plan Premium. */
    @PostMapping("/comprar")
    public ResponseEntity<?> comprarPremium() {
        try {
            planService.activarPremium();
            return ResponseEntity.ok(planService.obtenerEstado());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Cancela el Premium y vuelve al gratuito.
     *
     * Los errores salen como 400 y no como 500 por la misma razón que en la
     * compra: son rechazos previstos, no fallos del servidor.
     */
    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarPremium() {
        try {
            planService.cancelarPremium();
            return ResponseEntity.ok(planService.obtenerEstado());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}