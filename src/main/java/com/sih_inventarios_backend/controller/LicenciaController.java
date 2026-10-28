package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Licencia;
import com.sih_inventarios_backend.repository.LicenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/licencias")
public class LicenciaController {

    private final LicenciaRepository licenciaRepository;

    public LicenciaController(LicenciaRepository licenciaRepository) {
        this.licenciaRepository = licenciaRepository;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return licenciaRepository.findAll().stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("nombreSoftware", l.getNombreSoftware());
            m.put("fabricante", l.getFabricante());
            m.put("tipoLicencia", l.getTipoLicencia());
            m.put("cantidadAdquiridas", l.getCantidadAdquiridas());
            m.put("cantidadInstaladas", l.getCantidadInstaladas());
            m.put("fechaAdquisicion", l.getFechaAdquisicion());
            m.put("fechaVencimiento", l.getFechaVencimiento());
            m.put("estado", l.getEstado());
            m.put("observaciones", l.getObservaciones());
            m.put("tieneClave", l.getClaveLicencia() != null && !l.getClaveLicencia().isBlank());
            return m;
        }).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> crear(@RequestBody Licencia licencia) {
        try {
            licencia.setCantidadInstaladas(licencia.getCantidadInstaladas() == null ? 0 : licencia.getCantidadInstaladas());
            if (licencia.getClaveLicencia() != null && !licencia.getClaveLicencia().isBlank()) {
                // Se almacena como texto por ahora; en producción debe cifrarse con AES-256-GCM
                // y obtener clave desde variable de entorno
            }
            return ResponseEntity.ok(licenciaRepository.save(licencia));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Licencia cambios) {
        try {
            Licencia licencia = licenciaRepository.findById(id).orElseThrow(() -> new RuntimeException("La licencia no existe"));
            if (cambios.getNombreSoftware() != null) licencia.setNombreSoftware(cambios.getNombreSoftware());
            if (cambios.getFabricante() != null) licencia.setFabricante(cambios.getFabricante());
            if (cambios.getTipoLicencia() != null) licencia.setTipoLicencia(cambios.getTipoLicencia());
            if (cambios.getCantidadAdquiridas() != null) licencia.setCantidadAdquiridas(cambios.getCantidadAdquiridas());
            if (cambios.getFechaAdquisicion() != null) licencia.setFechaAdquisicion(cambios.getFechaAdquisicion());
            if (cambios.getFechaVencimiento() != null) licencia.setFechaVencimiento(cambios.getFechaVencimiento());
            if (cambios.getEstado() != null) licencia.setEstado(cambios.getEstado());
            if (cambios.getObservaciones() != null) licencia.setObservaciones(cambios.getObservaciones());
            if (cambios.getClaveLicencia() != null && !cambios.getClaveLicencia().isBlank()) {
                licencia.setClaveLicencia(cambios.getClaveLicencia());
            }
            return ResponseEntity.ok(licenciaRepository.save(licencia));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
