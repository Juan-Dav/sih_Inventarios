package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.*;
import com.sih_inventarios_backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custodia")
public class CustodiaController {

    private final EquipoRepository equipoRepository;
    private final AreaRepository areaRepository;
    private final DepartamentoRepository departamentoRepository;
    private final ResponsableCustodiaRepository responsableRepository;
    private final HistorialCustodiaRepository historialRepository;
    private final UsuarioRepository usuarioRepository;

    public CustodiaController(EquipoRepository equipoRepository, AreaRepository areaRepository,
                              DepartamentoRepository departamentoRepository, ResponsableCustodiaRepository responsableRepository,
                              HistorialCustodiaRepository historialRepository, UsuarioRepository usuarioRepository) {
        this.equipoRepository = equipoRepository;
        this.areaRepository = areaRepository;
        this.departamentoRepository = departamentoRepository;
        this.responsableRepository = responsableRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/historial/{equipoId}")
    public List<HistorialCustodia> historial(@PathVariable Long equipoId) {
        return historialRepository.findByEquipoIdOrderByFechaCambioDesc(equipoId);
    }

    @PostMapping("/asignar/{equipoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> asignar(@PathVariable Long equipoId,
                                     @RequestParam(required = false) Long areaId,
                                     @RequestParam(required = false) Long departamentoId,
                                     @RequestParam(required = false) Long responsableId,
                                     @RequestParam(required = false) String observaciones,
                                     Authentication authentication) {
        try {
            Equipo equipo = equipoRepository.findById(equipoId).orElseThrow(() -> new RuntimeException("El equipo no existe"));

            Area areaAnterior = equipo.getArea();
            Departamento departamentoAnterior = equipo.getDepartamento();
            ResponsableCustodia responsableAnterior = equipo.getResponsableCustodia();

            Area areaNueva = null;
            Departamento departamentoNuevo = null;
            ResponsableCustodia responsableNuevo = null;

            if (areaId != null) {
                areaNueva = areaRepository.findById(areaId).orElseThrow(() -> new RuntimeException("El área no existe"));
            }
            if (departamentoId != null) {
                departamentoNuevo = departamentoRepository.findById(departamentoId).orElseThrow(() -> new RuntimeException("El departamento no existe"));
                if (areaNueva != null && departamentoNuevo.getArea() != null && !departamentoNuevo.getArea().getId().equals(areaNueva.getId())) {
                    throw new RuntimeException("El departamento no pertenece al área seleccionada.");
                }
                if (areaNueva == null && departamentoNuevo.getArea() != null) {
                    areaNueva = departamentoNuevo.getArea();
                }
            }
            if (responsableId != null) {
                responsableNuevo = responsableRepository.findById(responsableId).orElseThrow(() -> new RuntimeException("El responsable no existe"));
            }

            HistorialCustodia hist = new HistorialCustodia();
            hist.setEquipo(equipo);
            hist.setAreaAnterior(areaAnterior);
            hist.setDepartamentoAnterior(departamentoAnterior);
            hist.setResponsableAnterior(responsableAnterior);
            hist.setAreaNueva(areaNueva);
            hist.setDepartamentoNuevo(departamentoNuevo);
            hist.setResponsableNuevo(responsableNuevo);
            hist.setFechaCambio(LocalDateTime.now());
            hist.setObservaciones(observaciones);
            if (authentication != null && authentication.getName() != null) {
                usuarioRepository.findByCorreo(authentication.getName()).ifPresent(hist::setUsuarioQueAsigna);
            }
            historialRepository.save(hist);

            equipo.setArea(areaNueva);
            equipo.setDepartamento(departamentoNuevo);
            equipo.setResponsableCustodia(responsableNuevo);
            equipoRepository.save(equipo);

            return ResponseEntity.ok(Map.of("message", "Asignación de custodia actualizada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
