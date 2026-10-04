package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.dto.VerificacionSalaResponse;
import com.sih_inventarios_backend.entity.Verificacion;
import com.sih_inventarios_backend.service.VerificacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Conteo físico de los equipos de cada sala.
 *
 * Es la labor del técnico: recorre el aula, cuenta lo que hay y deja constancia
 * de la diferencia con lo que dice el inventario. La consulta la puede hacer
 * cualquiera, porque el administrador necesita ver si un aula está al día.
 */
@RestController
@RequestMapping("/api/verificaciones")
public class VerificacionController {

    private final VerificacionService verificacionService;

    public VerificacionController(VerificacionService verificacionService) {
        this.verificacionService = verificacionService;
    }

    /**
     * Estado de verificación de todas las salas.
     *
     * Trae, por sala, el conteo registrado, el último conteo físico y la
     * diferencia entre ambos.
     */
    @GetMapping
    public List<VerificacionSalaResponse> listarEstadoPorSala() {
        return verificacionService.obtenerEstadoPorSala();
    }

    /** Historial completo de conteos. */
    @GetMapping("/historial")
    public List<Verificacion> listarHistorial() {
        return verificacionService.obtenerTodas();
    }

    /** Conteos de una sala concreta. */
    @GetMapping("/sala/{idSala}")
    public List<Verificacion> listarPorSala(@PathVariable Long idSala) {
        return verificacionService.obtenerPorSala(idSala);
    }

    /**
     * Registra el conteo de una sala.
     *
     * `cantidadEsperada` no viene del cliente: la calcula el servicio con los
     * equipos registrados. Lo que sí manda el técnico es `cantidadEncontrada`,
     * que es lo que contó a mano.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody Verificacion verificacion,
            @RequestParam Long idSala,
            @AuthenticationPrincipal String correo) {
        try {
            return ResponseEntity.ok(verificacionService.registrar(verificacion, idSala, correo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
