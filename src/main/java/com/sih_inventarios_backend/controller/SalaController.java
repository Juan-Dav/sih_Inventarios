package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Aulas o ubicaciones de la institución.
 *
 * Consulta para cualquier usuario autenticado (el docente necesita ver dónde
 * está cada equipo) y alta para los tres roles: quien llega al sitio es quien
 * registra el aula, así que dejar esa labor en manos de un técnico o de la
 * administración hacía que el docente tuviera que esperar para poder documentar
 * lo que ya vio. La edición, en cambio, es del técnico y de la administración:
 * corregir un aula ya registrada es trabajo sobre el inventario consolidado.
 */
@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    /**
     * Registra una sala.
     *
     * El alta puede fallar porque el plan ya tiene su límite de aulas, y eso no
     * es un fallo del servidor sino un rechazo previsto: por eso se captura y se
     * devuelve 400 con el mensaje, que es el que el frontend muestra. Sin el
     * try/catch el usuario vería un error 500 sin explicación.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'DOCENTE')")
    public ResponseEntity<?> crearSala(@Valid @RequestBody Sala sala) {
        try {
            return ResponseEntity.ok(salaService.guardarSala(sala));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public List<Sala> listarSalas() {
        return salaService.obtenerTodas();
    }

    // Modifica el nombre o la capacidad de una sala: PUT /api/salas/1
    //
    // Corregir un aula ya registrada es del técnico y de la administración.
    // El docente solo da de alta: puede documentar el aula que ve, pero no
    // reescribir lo que otros ya registraron.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizarSala(@PathVariable Long id, @Valid @RequestBody Sala sala) {
        try {
            return ResponseEntity.ok(salaService.actualizarSala(id, sala));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Elimina una sala. Solo técnicos y administradores.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> eliminarSala(@PathVariable Long id) {
        try {
            salaService.eliminarSala(id);
            return ResponseEntity.ok(Map.of("message", "Sala eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
