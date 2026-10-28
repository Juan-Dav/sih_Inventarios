package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Prestamo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.PrestamoRepository;
import com.sih_inventarios_backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    public PrestamoController(PrestamoRepository prestamoRepository, EquipoRepository equipoRepository, UsuarioRepository usuarioRepository) {
        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Prestamo> listar() {
        return prestamoRepository.findAll();
    }

    @GetMapping("/activos")
    public List<Prestamo> activos() {
        return prestamoRepository.findByEstado("PRESTADO");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Transactional
    public ResponseEntity<?> prestar(@RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            Long equipoId = Long.valueOf(body.get("equipoId").toString());
            Equipo equipo = equipoRepository.findById(equipoId).orElseThrow(() -> new RuntimeException("El equipo no existe"));
            List<Prestamo> activos = prestamoRepository.findByEquipoIdAndEstado(equipoId, "PRESTADO");
            if (!activos.isEmpty()) {
                throw new RuntimeException("El equipo ya tiene un préstamo activo.");
            }
            Prestamo prestamo = new Prestamo();
            prestamo.setEquipo(equipo);
            prestamo.setFechaSalida(LocalDateTime.now());
            String fechaEst = body.get("fechaDevolucionEstimada").toString();
            prestamo.setFechaDevolucionEstimada(LocalDateTime.parse(fechaEst));
            prestamo.setEstado("PRESTADO");
            prestamo.setObservaciones(body.get("observaciones") != null ? body.get("observaciones").toString() : null);
            if (authentication != null && authentication.getName() != null) {
                usuarioRepository.findByCorreo(authentication.getName()).ifPresent(prestamo::setUsuarioPrestamo);
            }
            prestamoRepository.save(prestamo);
            equipo.setEstado("EN_MANTENIMIENTO");
            equipoRepository.save(equipo);
            return ResponseEntity.ok(prestamo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Transactional
    public ResponseEntity<?> devolver(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Prestamo prestamo = prestamoRepository.findById(id).orElseThrow(() -> new RuntimeException("El préstamo no existe"));
            if (!"PRESTADO".equals(prestamo.getEstado())) {
                throw new RuntimeException("El préstamo ya fue devuelto o cancelado.");
            }
            prestamo.setFechaDevolucionReal(LocalDateTime.now());
            prestamo.setEstado("DEVUELTO");
            if (body != null && body.get("observacionesDevolucion") != null) {
                prestamo.setObservacionesDevolucion(body.get("observacionesDevolucion").toString());
            }
            prestamoRepository.save(prestamo);
            if (prestamo.getEquipo() != null) {
                Equipo equipo = prestamo.getEquipo();
                equipo.setEstado("OPERATIVO");
                equipoRepository.save(equipo);
            }
            return ResponseEntity.ok(prestamo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
