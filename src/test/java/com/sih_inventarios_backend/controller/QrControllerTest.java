package com.sih_inventarios_backend.controller;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.service.QrService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprueba que la imagen que se muestra en el modal contiene exactamente el
 * enlace a la ficha pública del frontend.
 *
 * No basta con que el endpoint responda 200: el problema reportado era que la
 * ventana se cerraba antes de poder escanear el QR, así que aquí se decodifica
 * el PNG generado con el mismo lector que usaría un teléfono.
 */
class QrControllerTest {

    private final EquipoRepository repositorio = mock(EquipoRepository.class);
    private final QrService qrService = new QrService(repositorio);
    private final QrController controller = new QrController(
            qrService, repositorio, "http://localhost:4200");

    @Test
    void laImagenSePuedeEscanearYApuntaALaFichaDelFrontend() throws Exception {
        Equipo equipo = equipo(7L);
        when(repositorio.findById(7L)).thenReturn(Optional.of(equipo));
        when(repositorio.save(any(Equipo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<byte[]> respuesta = controller.imagen(
                7L, 320, "http://localhost:4200", null);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("image/png", respuesta.getHeaders().getContentType().toString());
        assertNotNull(equipo.getCodigoQr());
        assertEquals(
                "http://localhost:4200/consulta/" + equipo.getCodigoQr(),
                leerQr(respuesta.getBody()));
    }

    @Test
    void usaElRefererCuandoElNavegadorNoMandaOrigin() throws Exception {
        Equipo equipo = equipo(8L);
        equipo.setCodigoQr("codigo-fijo");
        when(repositorio.findById(8L)).thenReturn(Optional.of(equipo));

        ResponseEntity<byte[]> respuesta = controller.imagen(
                8L, 320, null, "https://inventario.ejemplo.com/dashboard-admin/salas");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(
                "https://inventario.ejemplo.com/consulta/codigo-fijo",
                leerQr(respuesta.getBody()));
    }

    @Test
    void sinOrigenNiFrontendUrlElQrApuntaALaTablaDelBackend() throws Exception {
        QrController sinConfigurar = new QrController(qrService, repositorio, "");
        Equipo equipo = equipo(9L);
        equipo.setCodigoQr("codigo-fijo");
        when(repositorio.findById(9L)).thenReturn(Optional.of(equipo));

        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setScheme("https");
        peticion.setServerName("sih-inventarios.onrender.com");
        peticion.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));

        try {
            ResponseEntity<byte[]> respuesta = sinConfigurar.imagen(9L, 320, null, null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(
                    "https://sih-inventarios.onrender.com/api/consulta/qr/codigo-fijo",
                    leerQr(respuesta.getBody()));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void unEquipoInexistenteNoRompeLaRespuesta() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> respuesta = controller.imagen(99L, 320, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    private Equipo equipo(Long id) {
        Equipo equipo = new Equipo();
        equipo.setId(id);
        equipo.setCodigo("PC-00" + id);
        return equipo;
    }

    private String leerQr(byte[] png) throws Exception {
        assertNotNull(png);
        assertTrue(png.length > 0);
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        Result resultado = new QRCodeReader().decode(bitmap);
        return resultado.getText();
    }
}
