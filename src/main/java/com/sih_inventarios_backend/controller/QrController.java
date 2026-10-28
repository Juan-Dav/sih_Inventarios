package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.service.QrService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/equipos/{id}")
public class QrController {

    private final QrService qrService;
    private final EquipoRepository equipoRepository;

    public QrController(QrService qrService, EquipoRepository equipoRepository) {
        this.qrService = qrService;
        this.equipoRepository = equipoRepository;
    }

    @PostMapping("/generar-qr")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<?> generar(@PathVariable Long id) {
        try {
            Equipo equipo = qrService.generarQr(id);
            return ResponseEntity.ok(Map.of("codigoQr", equipo.getCodigoQr(), "rutaQr", equipo.getRutaQr()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/qr/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id, @RequestParam(defaultValue = "256") int size) {
        try {
            Equipo equipo = equipoRepository.findById(id).orElseThrow(() -> new RuntimeException("El equipo no existe"));
            if (equipo.getCodigoQr() == null || equipo.getCodigoQr().isBlank()) {
                equipo = qrService.generarQr(id);
            }
            // El QR guarda la URL completa de consulta, no solo el identificador:
            // al escanearlo con el celular el navegador abre la ficha del equipo.
            int lado = Math.max(64, Math.min(size, 1024));
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/consulta/qr/{codigo}")
                    .buildAndExpand(equipo.getCodigoQr())
                    .toUriString();
            byte[] image = qrService.generarImagen(url, lado, lado);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            return ResponseEntity.ok().headers(headers).body(image);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
