package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ficha pública de un equipo a partir del código QR.
 *
 * Es la consulta que abre el celular al escanear la etiqueta, así que no pide
 * sesión y devuelve solo información del equipo: nunca datos de cuentas,
 * correos, contraseñas ni configuración administrativa.
 *
 * Hay dos formas de llegar aquí:
 *
 * - El frontend pide el JSON para pintar la ficha (`/consulta/{codigo}`).
 * - Un QR impreso antes de este cambio apunta directo a esta ruta del backend.
 *   Si quien entra es un navegador (Accept: text/html), se le redirige a la
 *   página pública del frontend para que no vea el JSON crudo.
 *
 * Se responde sin caché para que, al volver a escanear, el estado que se vea
 * sea el actual y no el que tenía la primera vez.
 */
@RestController
@RequestMapping("/api/consulta/qr")
public class ConsultaQrController {

    private final EquipoRepository equipoRepository;

    /** URL pública del frontend, para redirigir los QR antiguos. */
    private final String frontendUrl;

    public ConsultaQrController(
            EquipoRepository equipoRepository,
            @Value("${app.frontend.url:}") String frontendUrl) {
        this.equipoRepository = equipoRepository;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/{codigoQr}")
    public ResponseEntity<?> consultar(
            @PathVariable String codigoQr,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acepta) {

        // Los QR viejos apuntan a esta URL. Un navegador pide HTML; se le lleva
        // a la ficha visual. El HttpClient de Angular pide JSON y sigue recibiendo
        // los datos, así que una misma ruta sirve para los dos casos.
        if (quiereHtml(acepta)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(urlFicha(codigoQr)))
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        Optional<Equipo> equipoOpt = equipoRepository.findByCodigoQr(codigoQr);

        if (equipoOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "Código QR no válido o equipo no encontrado"));
        }

        Equipo equipo = equipoOpt.get();

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", equipo.getId());
        datos.put("codigo", equipo.getCodigo());
        datos.put("codigoQr", equipo.getCodigoQr());
        datos.put("nombreEquipo", equipo.getNombreEquipo());
        datos.put("categoria", equipo.getCategoria());
        datos.put("caracteristicas", equipo.getCaracteristicas());
        datos.put("marca", equipo.getMarca());
        datos.put("modelo", equipo.getModelo());
        datos.put("numeroSerie", equipo.getNumeroSerie());
        datos.put("fechaAdquisicion", equipo.getFechaAdquisicion());
        datos.put("garantiaHasta", equipo.getGarantiaHasta());
        datos.put("ubicacionDetalle", equipo.getUbicacionDetalle());
        datos.put("observaciones", equipo.getObservaciones());
        datos.put("activo", equipo.getActivo());
        datos.put("estado", equipo.getEstado());

        if (equipo.getSala() != null) {
            datos.put("sala", equipo.getSala().getNombre());
        }
        if (equipo.getArea() != null) {
            datos.put("area", equipo.getArea().getNombre());
        }
        if (equipo.getDepartamento() != null) {
            datos.put("departamento", equipo.getDepartamento().getNombre());
        }

        // No se envía el responsable de custodia ni ningún dato de usuarios:
        // la ficha es pública y solo debe mostrar el equipo y su ubicación.

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(datos);
    }

    private boolean quiereHtml(String acepta) {
        return acepta != null && acepta.toLowerCase().contains("text/html");
    }

    private String urlFicha(String codigoQr) {
        String base = normalizarBase(frontendUrl);

        // Sin URL configurada se usa una ruta relativa: al menos el navegador
        // no se queda viendo el JSON del API.
        if (base == null) {
            return "/consulta/" + codigoQr;
        }

        return base + "/consulta/" + codigoQr;
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
