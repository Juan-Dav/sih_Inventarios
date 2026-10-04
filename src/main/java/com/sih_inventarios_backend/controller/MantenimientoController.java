package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Mantenimiento;
import com.sih_inventarios_backend.service.MantenimientoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Intervenciones técnicas: diagnóstico y solución de cada falla.
 *
 * Solo administración y técnico registran mantenimientos. El docente los
 * consulta, porque son la respuesta a lo que reportó, pero no escribe en el
 * historial: el registro de una intervención es del técnico que la hizo.
 */
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    public MantenimientoController(MantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> registrarMantenimiento(
            @Valid @RequestBody Mantenimiento mantenimiento,
            @RequestParam Long idEquipo,
            @RequestParam Long idFalla) {
        try {
            return ResponseEntity.ok(mantenimientoService.registrarMantenimiento(mantenimiento, idEquipo, idFalla));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public List<Mantenimiento> listarMantenimientos() {
        return mantenimientoService.obtenerTodos();
    }

    // Historial de un equipo: GET /api/mantenimientos/equipo/1
    @GetMapping("/equipo/{idEquipo}")
    public List<Mantenimiento> listarPorEquipo(@PathVariable Long idEquipo) {
        return mantenimientoService.obtenerPorEquipo(idEquipo);
    }
}
