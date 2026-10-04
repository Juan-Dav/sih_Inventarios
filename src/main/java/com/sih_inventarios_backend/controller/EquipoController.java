package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Equipos tecnológicos del inventario.
 *
 * La consulta es abierta a cualquier usuario autenticado, porque el docente
 * tiene que poder ver los equipos, dónde están y en qué estado. El alta también
 * es suya: es quien recibe la máquina en el aula y la documenta en el momento.
 * La edición, el cambio de estado y la atención de fallas quedan para el técnico
 * y la administración: son decisiones sobre el inventario ya consolidado y son
 * decisiones de mantenimiento, no de inventario.
 */
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    // Se registra enviando los datos del equipo y el ID de la sala por la URL: /api/equipos?idSala=1
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'DOCENTE')")
    public ResponseEntity<?> crearEquipo(@Valid @RequestBody Equipo equipo, @RequestParam Long idSala) {
        try {
            Equipo nuevoEquipo = equipoService.registrarEquipo(equipo, idSala);
            return ResponseEntity.ok(nuevoEquipo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public List<Equipo> listarEquipos() {
        return equipoService.obtenerTodos();
    }

    /**
     * Equipos filtrados por estado.
     *
     * Es el "consultar el estado actual de los equipos" del administrador: el
     * backend valida el nombre del estado y devuelve 400 si no es uno de los
     * tres, en vez de una lista vacía que parecería "no hay equipos".
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            return ResponseEntity.ok(equipoService.obtenerPorEstado(estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Equipos fuera de servicio.
     *
     * Lo consumen el administrador (supervisión) y el técnico (seguimiento), por
     * eso es un atajo y no un filtro `estado=FUERA_DE_SERVICIO` que habría que
     * escribir a mano en la barra de direcciones.
     */
    @GetMapping("/fuera-de-servicio")
    public List<Equipo> listarFueraDeServicio() {
        return equipoService.obtenerFueraDeServicio();
    }

    // Equipos de una sala concreta: GET /api/equipos/sala/1
    @GetMapping("/sala/{idSala}")
    public List<Equipo> listarPorSala(@PathVariable Long idSala) {
        return equipoService.obtenerPorSala(idSala);
    }

    // Cambia el estado del equipo: PATCH /api/equipos/1/estado?estado=OPERATIVO
    //
    // A diferencia del alta, aquí el docente no interviene: poner una máquina
    // fuera de servicio es una decisión de mantenimiento, y dejarla en sus manos
    // haría que cualquiera pudiera deshabilitar el equipo de otro aula.
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
        try {
            return ResponseEntity.ok(equipoService.actualizarEstado(id, estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Modifica código, características, estado o aula: PUT /api/equipos/1
    //
    // Igual que el alta, aquí el docente no interviene: una vez registrado, el
    // equipo pasa a formar parte del inventario consolidado y corregirlo (o
    // moverlo de aula) le toca al técnico o a la administración.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizarEquipo(
            @PathVariable Long id,
            @Valid @RequestBody Equipo equipo,
            @RequestParam(required = false) Long idSala) {
        try {
            return ResponseEntity.ok(equipoService.actualizarEquipo(id, equipo, idSala));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Historial del equipo: GET /api/equipos/1/historial
    @GetMapping("/{id}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(equipoService.obtenerHistorial(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
