package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Area;
import com.sih_inventarios_backend.repository.AreaRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaRepository areaRepository;

    public AreaController(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @GetMapping
    public List<Area> listar() {
        return areaRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> crear(@Valid @RequestBody Area area) {
        try {
            area.setActivo(area.getActivo() == null ? true : area.getActivo());
            return ResponseEntity.ok(areaRepository.save(area));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Area cambios) {
        try {
            Area area = areaRepository.findById(id).orElseThrow(() -> new RuntimeException("El área no existe"));
            if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
                area.setNombre(cambios.getNombre());
            }
            area.setDescripcion(cambios.getDescripcion());
            if (cambios.getActivo() != null) {
                area.setActivo(cambios.getActivo());
            }
            return ResponseEntity.ok(areaRepository.save(area));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
