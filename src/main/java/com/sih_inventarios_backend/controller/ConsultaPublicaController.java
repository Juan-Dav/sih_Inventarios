package com.sih_inventarios_backend.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Alias público de la ficha del QR.
 *
 * Algunas etiquetas ya impresas apuntan a `/consulta/{codigo}` sobre el dominio
 * del backend (no al del frontend). Esa ruta no existe en el API, así que el
 * celular mostraba un error en JSON. Aquí se redirige a la ruta que sí pinta la
 * tabla HTML, de modo que cualquier QR antiguo sigue siendo legible.
 */
@RestController
public class ConsultaPublicaController {

    @GetMapping("/consulta/{codigoQr}")
    public ResponseEntity<Void> ficha(@PathVariable String codigoQr) {
        String codigo = UriUtils.encodePathSegment(codigoQr, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/api/consulta/qr/" + codigo))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
