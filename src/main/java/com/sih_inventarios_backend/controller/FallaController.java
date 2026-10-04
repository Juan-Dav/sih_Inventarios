package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.service.FallaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Fallas de los equipos.
 *
 * El alta es el único endpoint abierto a los tres roles: el docente reporta la
 * novedad que encuentra y el técnico da de alta las que detecta él. Mover una
 * falla de PENDIENTE a EN_REVISION o RESUELTO, en cambio, es parte del trabajo
 * técnico, así que el docente no puede hacerlo aunque se le ocurra.
 */
@RestController
@RequestMapping("/api/fallas")
public class FallaController {

    private final FallaService fallaService;

    public FallaController(FallaService fallaService) {
        this.fallaService = fallaService;
    }

    @PostMapping
    public ResponseEntity<?> reportarFalla(@Valid @RequestBody Falla falla, @RequestParam Long idEquipo) {
        try {
            return ResponseEntity.ok(fallaService.reportarFalla(falla, idEquipo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public List<Falla> listarFallas() {
        return fallaService.obtenerTodas();
    }

    /**
     * Fallas que siguen abiertas.
     *
     * Es la cola de trabajo del técnico y el listado que el docente consulta
     * para ver en qué estado están los problemas reportados.
     */
    @GetMapping("/abiertas")
    public List<Falla> listarAbiertas() {
        return fallaService.obtenerAbiertas();
    }

    // Fallas de un estado concreto: GET /api/fallas/estado/PENDIENTE
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            return ResponseEntity.ok(fallaService.obtenerPorEstado(estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Cambia el estado de la falla: PATCH /api/fallas/1/estado?estado=RESUELTO
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
        try {
            return ResponseEntity.ok(fallaService.actualizarEstado(id, estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
