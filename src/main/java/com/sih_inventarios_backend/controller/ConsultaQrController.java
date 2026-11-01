package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
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
 * - El frontend pide el JSON para pintar su ficha visual (`/consulta/{codigo}`).
 * - Un QR impreso puede apuntar directo a esta ruta del backend. En ese caso
 *   NO se responde con el JSON crudo: se pinta una tabla HTML legible. Así una
 *   etiqueta antigua sigue sirviendo aunque el frontend esté en otra URL o no
 *   se haya configurado FRONTEND_URL en el despliegue.
 *
 * Se responde sin caché para que, al volver a escanear, el estado que se vea
 * sea el actual y no el que tenía la primera vez.
 *
 * El `{codigoQr}` puede ser el UUID guardado en `codigoQr` (lo que llevan las
 * etiquetas) o el `id` numérico del equipo.
 */
@RestController
@RequestMapping("/api/consulta/qr")
public class ConsultaQrController {

    private final EquipoRepository equipoRepository;

    public ConsultaQrController(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @GetMapping("/{codigoQr}")
    public ResponseEntity<?> consultar(
            @PathVariable String codigoQr,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acepta) {

        Optional<Equipo> equipoOpt = buscarEquipo(codigoQr);

        // Un navegador debe ver la ficha, nunca el JSON del API. Se acepta
        // también `*/*` porque algunas apps de escaneo no piden HTML de forma
        // explícita. El HttpClient de Angular sí manda application/json y
        // sigue recibiendo los datos para pintar su propia página.
        if (quiereHtml(acepta)) {
            return equipoOpt
                    .map(equipo -> ResponseEntity.ok()
                            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                            .cacheControl(CacheControl.noStore())
                            .header("X-Content-Type-Options", "nosniff")
                            .body(paginaEquipo(equipo)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                            .cacheControl(CacheControl.noStore())
                            .header("X-Content-Type-Options", "nosniff")
                            .body(paginaNoEncontrado(codigoQr)));
        }

        if (equipoOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "Código QR no válido o equipo no encontrado"));
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(datosJson(equipoOpt.get()));
    }

    /**
     * Busca el equipo por el identificador que viaja en la URL.
     *
     * El QR usa `codigoQr`, un UUID único guardado en la base de datos. Se
     * acepta además el `id` numérico para que un enlace armado a mano (o una
     * etiqueta antigua que usara el id) siga abriendo la misma ficha.
     */
    private Optional<Equipo> buscarEquipo(String codigoQr) {
        if (codigoQr == null || codigoQr.isBlank()) {
            return Optional.empty();
        }

        String codigo = codigoQr.trim();
        Optional<Equipo> porCodigo = equipoRepository.findByCodigoQr(codigo);
        if (porCodigo.isPresent()) {
            return porCodigo;
        }

        if (codigo.matches("\\d{1,18}")) {
            try {
                return equipoRepository.findById(Long.parseLong(codigo));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /** Datos que consume la ficha visual de Angular. */
    private Map<String, Object> datosJson(Equipo equipo) {
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
        return datos;
    }

    private boolean quiereHtml(String acepta) {
        if (acepta == null || acepta.isBlank()) {
            return true;
        }

        String valor = acepta.toLowerCase(Locale.ROOT);

        // El cliente HTTP del frontend pide JSON de forma explícita.
        if (valor.contains("application/json")) {
            return false;
        }

        return valor.contains("text/html") || valor.contains("*/*");
    }

    private String paginaEquipo(Equipo equipo) {
        String codigo = texto(equipo.getCodigo(), "Sin código");
        String nombre = texto(equipo.getNombreEquipo(), nombreVisible(equipo));
        String ubicacion = ubicacion(equipo);
        String estado = estadoTexto(equipo.getEstado());
        String claseEstado = estadoClase(equipo.getEstado());

        StringBuilder filas = new StringBuilder();
        filas.append(fila("Código", codigo));
        filas.append(fila("Nombre", nombre));
        filas.append(fila("Categoría", equipo.getCategoria()));
        filas.append(fila("Marca", equipo.getMarca()));
        filas.append(fila("Modelo", equipo.getModelo()));
        filas.append(fila("Número de serie", equipo.getNumeroSerie()));
        filas.append(fila("Estado", estado));
        filas.append(fila("Registro", Boolean.FALSE.equals(equipo.getActivo()) ? "Inactivo" : "Activo"));
        filas.append(fila("Aula", equipo.getSala() != null ? equipo.getSala().getNombre() : null));
        filas.append(fila("Área", equipo.getArea() != null ? equipo.getArea().getNombre() : null));
        filas.append(fila("Departamento",
                equipo.getDepartamento() != null ? equipo.getDepartamento().getNombre() : null));
        filas.append(fila("Ubicación detallada", equipo.getUbicacionDetalle()));
        filas.append(fila("Fecha de adquisición", fecha(equipo.getFechaAdquisicion())));
        filas.append(fila("Garantía hasta", fecha(equipo.getGarantiaHasta())));
        filas.append(fila("Características", equipo.getCaracteristicas()));
        filas.append(fila("Observaciones", equipo.getObservaciones()));

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Equipo %s · SIH Inventarios</title>
                <style>
                  :root { color-scheme: dark; }
                  * { box-sizing: border-box; }
                  body {
                    margin: 0;
                    min-height: 100vh;
                    padding: 24px 16px;
                    font-family: "Segoe UI", Roboto, Arial, sans-serif;
                    color: #eef2ff;
                    background: radial-gradient(circle at top, #243b7a 0, #0d1230 48%%, #080a1c 100%%);
                  }
                  .hoja { width: min(760px, 100%%); margin: 0 auto; }
                  .marca {
                    margin: 0 0 14px;
                    font-size: .72rem;
                    font-weight: 800;
                    letter-spacing: .22em;
                    text-transform: uppercase;
                    color: #93c5fd;
                  }
                  .cabecera {
                    padding: 24px;
                    border-radius: 22px;
                    border: 1px solid rgba(255, 255, 255, .14);
                    background: linear-gradient(135deg, rgba(37, 99, 235, .34), rgba(124, 58, 237, .22));
                    box-shadow: 0 24px 70px rgba(0, 0, 0, .35);
                  }
                  .chip {
                    display: inline-block;
                    padding: 7px 12px;
                    border-radius: 999px;
                    font-size: .75rem;
                    font-weight: 800;
                    letter-spacing: .06em;
                    text-transform: uppercase;
                  }
                  .operativo { color: #052e16; background: #86efac; }
                  .mantenimiento { color: #422006; background: #fde68a; }
                  .fuera { color: #450a0a; background: #fca5a5; }
                  .otro { color: #e0e7ff; background: #4338ca; }
                  h1 { margin: 18px 0 6px; font-size: clamp(1.8rem, 6vw, 2.6rem); letter-spacing: -.03em; }
                  .nombre { margin: 0; font-size: 1.05rem; color: #dbeafe; }
                  .lugar { margin: 8px 0 0; color: #93a5cf; font-size: .9rem; }
                  .tarjeta {
                    margin-top: 18px;
                    padding: 22px;
                    border-radius: 22px;
                    border: 1px solid rgba(255, 255, 255, .12);
                    background: rgba(15, 23, 42, .78);
                  }
                  h2 { margin: 0 0 14px; font-size: 1.05rem; color: #bfdbfe; }
                  table { width: 100%%; border-collapse: collapse; }
                  th, td { padding: 11px 10px; border-bottom: 1px solid rgba(255, 255, 255, .09); text-align: left; vertical-align: top; }
                  th { width: 42%%; color: #93a5cf; font-size: .78rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; }
                  td { font-size: .93rem; word-break: break-word; }
                  tr:last-child th, tr:last-child td { border-bottom: 0; }
                  .pie { margin: 18px 0 0; text-align: center; color: #64748b; font-size: .75rem; }
                  a { color: #93c5fd; }
                </style>
                </head>
                <body>
                  <main class="hoja">
                    <p class="marca">SIH Inventarios · Ficha escaneada</p>
                    <header class="cabecera">
                      <span class="chip %s">%s</span>
                      <h1>%s</h1>
                      <p class="nombre">%s</p>
                      <p class="lugar">%s</p>
                    </header>
                    <section class="tarjeta">
                      <h2>Información del equipo</h2>
                      <table>
                        <tbody>%s</tbody>
                      </table>
                    </section>
                    <p class="pie">Información del inventario institucional · SIH Inventarios</p>
                  </main>
                </body>
                </html>
                """
                .formatted(
                        html(codigo),
                        claseEstado,
                        html(estado),
                        html(codigo),
                        html(nombre),
                        html(ubicacion),
                        filas);
    }

    private String paginaNoEncontrado(String codigoQr) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Equipo no encontrado · SIH Inventarios</title>
                <style>
                  * { box-sizing: border-box; }
                  body {
                    margin: 0;
                    min-height: 100vh;
                    display: grid;
                    place-items: center;
                    padding: 24px;
                    font-family: "Segoe UI", Roboto, Arial, sans-serif;
                    color: #eef2ff;
                    background: radial-gradient(circle at top, #243b7a 0, #0d1230 48%%, #080a1c 100%%);
                  }
                  .tarjeta {
                    width: min(560px, 100%%);
                    padding: 32px;
                    border-radius: 22px;
                    border: 1px solid rgba(255, 255, 255, .14);
                    background: rgba(15, 23, 42, .82);
                    box-shadow: 0 24px 70px rgba(0, 0, 0, .35);
                    text-align: center;
                  }
                  .icono { font-size: 2.2rem; }
                  h1 { margin: 10px 0; font-size: 1.5rem; }
                  p { color: #93a5cf; line-height: 1.6; }
                  code { color: #fca5a5; word-break: break-all; }
                </style>
                </head>
                <body>
                  <main class="tarjeta">
                    <p class="icono">⚠️</p>
                    <h1>Equipo no encontrado</h1>
                    <p>
                      El código <code>%s</code> no corresponde a ningún equipo del inventario.
                      Puede que la etiqueta sea antigua o que el equipo haya sido retirado.
                    </p>
                  </main>
                </body>
                </html>
                """
                .formatted(html(codigoQr));
    }

    private String fila(String etiqueta, Object valor) {
        return "<tr><th>" + html(etiqueta) + "</th><td>" + html(texto(valor, "No especificado"))
                + "</td></tr>";
    }

    private String nombreVisible(Equipo equipo) {
        String marca = texto(equipo.getMarca(), "");
        String modelo = texto(equipo.getModelo(), "");
        String nombre = (marca + " " + modelo).trim();
        return nombre.isBlank() ? "Equipo sin nombre registrado" : nombre;
    }

    private String ubicacion(Equipo equipo) {
        StringBuilder lugar = new StringBuilder();
        if (equipo.getSala() != null) {
            agregar(lugar, equipo.getSala().getNombre());
        }
        if (equipo.getArea() != null) {
            agregar(lugar, equipo.getArea().getNombre());
        }
        if (equipo.getDepartamento() != null) {
            agregar(lugar, equipo.getDepartamento().getNombre());
        }
        return lugar.isEmpty() ? "Sin ubicación registrada" : lugar.toString();
    }

    private void agregar(StringBuilder lugar, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!lugar.isEmpty()) {
            lugar.append(" · ");
        }
        lugar.append(valor.trim());
    }

    private String estadoTexto(String estado) {
        String valor = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "OPERATIVO" -> "Operativo";
            case "EN_MANTENIMIENTO" -> "En mantenimiento";
            case "FUERA_DE_SERVICIO" -> "Fuera de servicio";
            default -> valor.isBlank()
                    ? "Sin estado registrado"
                    : valor.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private String estadoClase(String estado) {
        String valor = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "OPERATIVO" -> "operativo";
            case "EN_MANTENIMIENTO" -> "mantenimiento";
            case "FUERA_DE_SERVICIO" -> "fuera";
            default -> "otro";
        };
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private String texto(Object valor, String porDefecto) {
        if (valor == null) {
            return porDefecto;
        }
        String limpio = valor.toString().trim();
        return limpio.isEmpty() ? porDefecto : limpio;
    }

    /** Evita que un dato del inventario rompa el HTML de la ficha. */
    private String html(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
