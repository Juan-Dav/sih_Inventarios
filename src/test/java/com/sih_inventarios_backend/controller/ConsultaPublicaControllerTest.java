package com.sih_inventarios_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Las etiquetas antiguas pueden apuntar a `/consulta/{codigo}` en el dominio
 * del backend; esa ruta debe llevar a la tabla HTML y no dejar al celular en
 * una página de error.
 */
class ConsultaPublicaControllerTest {

    private final ConsultaPublicaController controller = new ConsultaPublicaController();

    @Test
    void redirigeLaRutaAntiguaALaFichaHtmlDelBackend() {
        ResponseEntity<Void> respuesta = controller.ficha("bfd733d2-64c1-4193-a049-02b9027fca2b");

        assertEquals(HttpStatus.FOUND, respuesta.getStatusCode());
        assertNotNull(respuesta.getHeaders().getLocation());
        assertEquals(
                "/api/consulta/qr/bfd733d2-64c1-4193-a049-02b9027fca2b",
                respuesta.getHeaders().getLocation().toString());
    }
}
