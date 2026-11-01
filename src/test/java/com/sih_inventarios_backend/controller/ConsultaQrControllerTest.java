package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Area;
import com.sih_inventarios_backend.entity.Departamento;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La ruta pública del QR debe responder una tabla legible cuando la abre un
 * navegador. El JSON crudo solo se reserva para el HttpClient del frontend.
 */
class ConsultaQrControllerTest {

    private final EquipoRepository repositorio = mock(EquipoRepository.class);
    private final ConsultaQrController controller = new ConsultaQrController(repositorio);

    private static final String NAVEGADOR =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String CLIENTE_JSON = "application/json, text/plain, */*";

    @Test
    void unNavegadorRecibeUnaTablaHtmlConLaInformacionDelEquipo() {
        when(repositorio.findByCodigoQr("codigo-1")).thenReturn(Optional.of(equipoCompleto()));

        ResponseEntity<?> respuesta = controller.consultar("codigo-1", NAVEGADOR);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("text/html;charset=UTF-8", respuesta.getHeaders().getContentType().toString());

        String html = assertInstanceOf(String.class, respuesta.getBody());
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("Información del equipo"));
        assertTrue(html.contains("PC-001"));
        assertTrue(html.contains("Aula 101"));
        assertTrue(html.contains("Operativo"));
        assertFalse(html.contains("\"message\""));
    }

    @Test
    void elFrontendSigueRecibiendoJsonParaPintarSuPropiaFicha() {
        when(repositorio.findByCodigoQr("codigo-1")).thenReturn(Optional.of(equipoCompleto()));

        ResponseEntity<?> respuesta = controller.consultar("codigo-1", CLIENTE_JSON);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        Map<?, ?> datos = assertInstanceOf(Map.class, respuesta.getBody());
        assertEquals("PC-001", datos.get("codigo"));
        assertEquals("Aula 101", datos.get("sala"));
    }

    @Test
    void tambienAceptaElIdNumericoDelEquipo() {
        when(repositorio.findByCodigoQr("1")).thenReturn(Optional.empty());
        when(repositorio.findById(1L)).thenReturn(Optional.of(equipoCompleto()));

        ResponseEntity<?> respuesta = controller.consultar("1", CLIENTE_JSON);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        Map<?, ?> datos = assertInstanceOf(Map.class, respuesta.getBody());
        assertEquals("PC-001", datos.get("codigo"));
    }

    @Test
    void unQrAntiguoInexistenteMuestraUnaPaginaDeErrorYNoJson() {
        when(repositorio.findByCodigoQr("codigo-viejo")).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.consultar("codigo-viejo", NAVEGADOR);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        String html = assertInstanceOf(String.class, respuesta.getBody());
        assertTrue(html.contains("Equipo no encontrado"));
        assertTrue(html.contains("codigo-viejo"));
        assertFalse(html.contains("\"message\""));
    }

    @Test
    void escapaLosDatosDelInventarioAntesDePintarlos() {
        Equipo equipo = equipoCompleto();
        equipo.setNombreEquipo("<script>alert('xss')</script>");
        equipo.setObservaciones("Texto & <b>resaltado</b>");
        when(repositorio.findByCodigoQr("codigo-1")).thenReturn(Optional.of(equipo));

        String html = assertInstanceOf(
                String.class, controller.consultar("codigo-1", NAVEGADOR).getBody());

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertTrue(html.contains("Texto &amp; &lt;b&gt;resaltado&lt;/b&gt;"));
    }

    private Equipo equipoCompleto() {
        Sala sala = new Sala();
        sala.setId(1L);
        sala.setNombre("Aula 101");
        sala.setCapacidadMaxima(20);

        Area area = new Area();
        area.setId(1L);
        area.setNombre("Tecnología");

        Departamento departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNombre("Sistemas");

        Equipo equipo = new Equipo();
        equipo.setId(1L);
        equipo.setCodigo("PC-001");
        equipo.setCodigoQr("codigo-1");
        equipo.setNombreEquipo("Equipo de prueba");
        equipo.setCategoria("Computador");
        equipo.setMarca("Lenovo");
        equipo.setModelo("ThinkCentre");
        equipo.setNumeroSerie("SN-123");
        equipo.setCaracteristicas("Intel i5, 16 GB RAM");
        equipo.setObservaciones("Sin novedades");
        equipo.setUbicacionDetalle("Laboratorio 2");
        equipo.setFechaAdquisicion(LocalDate.of(2025, 1, 15));
        equipo.setGarantiaHasta(LocalDate.of(2027, 1, 15));
        equipo.setActivo(true);
        equipo.setEstado("OPERATIVO");
        equipo.setSala(sala);
        equipo.setArea(area);
        equipo.setDepartamento(departamento);
        return equipo;
    }
}
