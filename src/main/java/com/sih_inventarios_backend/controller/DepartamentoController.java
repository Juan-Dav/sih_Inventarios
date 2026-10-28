package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Area;
import com.sih_inventarios_backend.entity.Departamento;
import com.sih_inventarios_backend.repository.AreaRepository;
import com.sih_inventarios_backend.repository.DepartamentoRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController {

    private final DepartamentoRepository departamentoRepository;
    private final AreaRepository areaRepository;

    public DepartamentoController(DepartamentoRepository departamentoRepository, AreaRepository areaRepository) {
        this.departamentoRepository = departamentoRepository;
        this.areaRepository = areaRepository;
    }

    @GetMapping
    public List<Departamento> listar(@RequestParam(required = false) Long areaId) {
        if (areaId != null) {
            return departamentoRepository.findByAreaId(areaId);
        }
        return departamentoRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> crear(@Valid @RequestBody Departamento departamento, @RequestParam Long areaId) {
        try {
            Area area = areaRepository.findById(areaId).orElseThrow(() -> new RuntimeException("El área no existe"));
            departamento.setArea(area);
            departamento.setActivo(departamento.getActivo() == null ? true : departamento.getActivo());
            return ResponseEntity.ok(departamentoRepository.save(departamento));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Departamento cambios, @RequestParam(required = false) Long areaId) {
        try {
            Departamento departamento = departamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("El departamento no existe"));
            if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
                departamento.setNombre(cambios.getNombre());
            }
            departamento.setDescripcion(cambios.getDescripcion());
            if (areaId != null) {
                Area area = areaRepository.findById(areaId).orElseThrow(() -> new RuntimeException("El área no existe"));
                departamento.setArea(area);
            }
            if (cambios.getActivo() != null) {
                departamento.setActivo(cambios.getActivo());
            }
            return ResponseEntity.ok(departamentoRepository.save(departamento));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
