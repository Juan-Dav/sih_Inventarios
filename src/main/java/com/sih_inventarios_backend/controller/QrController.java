package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.service.QrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/equipos/{id}")
public class QrController {

    private final QrService qrService;
    private final EquipoRepository equipoRepository;

    /**
     * URL pública del frontend, para que el QR apunte a la ficha y no al JSON
     * del API. Si no se configura se usa el origen desde el que se pidió la
     * imagen, que es lo normal cuando se genera desde el panel.
     */
    private final String frontendUrl;

    public QrController(
            QrService qrService,
            EquipoRepository equipoRepository,
            @Value("${app.frontend.url:}") String frontendUrl) {
        this.qrService = qrService;
        this.equipoRepository = equipoRepository;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/generar-qr")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'DOCENTE')")
    public ResponseEntity<?> generar(@PathVariable Long id) {
        try {
            Equipo equipo = qrService.generarQr(id);
            return ResponseEntity.ok(Map.of("codigoQr", equipo.getCodigoQr(), "rutaQr", equipo.getRutaQr()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/qr/imagen")
    public ResponseEntity<byte[]> imagen(
            @PathVariable Long id,
            @RequestParam(defaultValue = "256") int size,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origen,
            @RequestHeader(value = HttpHeaders.REFERER, required = false) String referente) {
        try {
            Equipo equipo = equipoRepository.findById(id).orElseThrow(() -> new RuntimeException("El equipo no existe"));
            if (equipo.getCodigoQr() == null || equipo.getCodigoQr().isBlank()) {
                equipo = qrService.generarQr(id);
            }
            // El QR guarda la URL de la ficha del frontend, no la del API: al
            // escanearlo con el celular debe abrirse una página legible con toda
            // la información del equipo, no el JSON de la consulta.
            int lado = Math.max(64, Math.min(size, 1024));
            String url = urlFicha(equipo.getCodigoQr(), origen, referente);
            byte[] image = qrService.generarImagen(url, lado, lado);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            return ResponseEntity.ok().headers(headers).body(image);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * URL que se codifica dentro del QR.
     *
     * El navegador manda el origen del frontend al pedir la imagen, así que el
     * enlace queda correcto tanto en local como en el despliegue sin tener que
     * cambiar código. `app.frontend.url` sirve de respaldo cuando la imagen se
     * genera fuera del navegador (por ejemplo, desde una herramienta o un script).
     */
    private String urlFicha(String codigoQr, String origen, String referente) {
        String base = normalizarBase(origen);

        if (base == null) {
            base = origenDe(referente);
        }

        if (base == null) {
            base = normalizarBase(frontendUrl);
        }

        if (base == null) {
            base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        return base + "/consulta/" + codigoQr;
    }

    /**
     * Saca el origen del `Referer`.
     *
     * Un `<img>` normal no manda `Origin`, pero sí `Referer`. Así el QR se arma
     * con la dirección real desde la que se está viendo el panel aunque no se
     * haya configurado `app.frontend.url`.
     */
    private String origenDe(String referente) {
        if (referente == null || referente.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(referente.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            String puerto = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + puerto;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Quita espacios y la barra final; devuelve `null` si no hay valor usable. */
    private String normalizarBase(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        while (limpio.endsWith("/")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }

        return limpio.isBlank() ? null : limpio;
    }
}
