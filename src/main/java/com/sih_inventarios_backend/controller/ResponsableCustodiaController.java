package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.ResponsableCustodia;
import com.sih_inventarios_backend.repository.ResponsableCustodiaRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/responsables-custodia")
public class ResponsableCustodiaController {

    private final ResponsableCustodiaRepository responsableRepository;

    public ResponsableCustodiaController(ResponsableCustodiaRepository responsableRepository) {
        this.responsableRepository = responsableRepository;
    }

    @GetMapping
    public List<ResponsableCustodia> listar() {
        return responsableRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> crear(@Valid @RequestBody ResponsableCustodia responsable) {
        try {
            responsable.setActivo(responsable.getActivo() == null ? true : responsable.getActivo());
            return ResponseEntity.ok(responsableRepository.save(responsable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody ResponsableCustodia cambios) {
        try {
            ResponsableCustodia responsable = responsableRepository.findById(id).orElseThrow(() -> new RuntimeException("El responsable no existe"));
            if (cambios.getNombreCompleto() != null && !cambios.getNombreCompleto().isBlank()) {
                responsable.setNombreCompleto(cambios.getNombreCompleto());
            }
            responsable.setCargo(cambios.getCargo());
            responsable.setTipoResponsable(cambios.getTipoResponsable());
            responsable.setUsuario(cambios.getUsuario());
            if (cambios.getActivo() != null) {
                responsable.setActivo(cambios.getActivo());
            }
            return ResponseEntity.ok(responsableRepository.save(responsable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
