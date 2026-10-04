package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.dto.VerificacionSalaResponse;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.entity.Usuario;
import com.sih_inventarios_backend.entity.Verificacion;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
import com.sih_inventarios_backend.repository.UsuarioRepository;
import com.sih_inventarios_backend.repository.VerificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Conteo físico de los equipos de cada sala.
 *
 * El técnico recorre el aula, cuenta lo que hay y lo compara con lo que el
 * inventario afirma. Aquí solo se registra el conteo: cambiar el estado de un
 * equipo o dar de baja uno que desapareció es trabajo del módulo de inventario,
 * para que el conteo no decida por su cuenta qué equipo se borra.
 */
@Service
public class VerificacionService {

    private static final String OPERATIVO = "OPERATIVO";
    private static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO";
    private static final String FUERA_DE_SERVICIO = "FUERA_DE_SERVICIO";

    private final VerificacionRepository verificacionRepository;
    private final SalaRepository salaRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    public VerificacionService(
            VerificacionRepository verificacionRepository,
            SalaRepository salaRepository,
            EquipoRepository equipoRepository,
            UsuarioRepository usuarioRepository) {
        this.verificacionRepository = verificacionRepository;
        this.salaRepository = salaRepository;
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra el conteo de una sala.
     *
     * `cantidadEsperada` se sobrescribe con el número de equipos registrados:
     * si se aceptara el que envía el cliente, un conteo podría "encontrar" más
     * equipos de los que existen y la diferencia siempre saldría negativa, que
     * es justo lo que el conteo debe detectar.
     */
    @Transactional
    public Verificacion registrar(Verificacion verificacion, Long idSala, String correoTecnico) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new RuntimeException("Error: La sala especificada no existe."));

        if (verificacion.getCantidadEncontrada() < 0) {
            throw new RuntimeException("Error: La cantidad encontrada no puede ser negativa.");
        }

        int registrados = (int) equipoRepository.countBySalaId(sala.getId());

        verificacion.setCantidadEsperada(registrados);
        verificacion.setSala(sala);
        verificacion.setTecnico(usuarioDelCorreo(correoTecnico));
        verificacion.setFechaVerificacion(LocalDateTime.now());

        // Los códigos llegan como texto ("PC-01, PC-02") porque el técnico los
        // escribe en el campo. Se limpian antes de guardar para que un espacio de
        // más no acabe dentro del reporte de equipos faltantes.
        verificacion.setEquiposFaltantes(normalizar(verificacion.getEquiposFaltantes()));
        verificacion.setEquiposDanados(normalizar(verificacion.getEquiposDanados()));
        verificacion.setObservaciones(normalizar(verificacion.getObservaciones()));

        return verificacionRepository.save(verificacion);
    }

    /**
     * Limpia un texto corto que viene de un campo de formulario.
     *
     * Quita los espacios de los extremos y de alrededor de cada coma, y joins
     * las comas que se hayan escrito de más. Si no queda nada, devuelve `null`
     * para no llenar la columna con cadenas vacías.
     */
    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String limpio = Arrays.stream(texto.split(","))
                .map(codigo -> codigo.trim())
                .filter(codigo -> !codigo.isEmpty())
                .collect(Collectors.joining(", "));

        return limpio.isEmpty() ? null : limpio;
    }

    /**
     * Estado de verificación de todas las salas.
     *
     * Trae también los equipos de cada sala porque el técnico necesita ver la
     * lista junto al conteo para saber cuál de los esperados no apareció.
     */
    @Transactional(readOnly = true)
    public List<VerificacionSalaResponse> obtenerEstadoPorSala() {
        // Tres consultas en total, sin importar cuántas aulas haya: antes se
        // hacían cinco consultas por cada sala (una por cada conteo de estado y
        // una para traer sus equipos), de modo que el módulo se volvía cada vez
        // más lento a medida que crecía el inventario.
        Map<Long, List<Verificacion>> porSala = verificacionRepository
                .findAllConDetalles()
                .stream()
                .collect(Collectors.groupingBy(verificacion -> verificacion.getSala().getId()));

        Map<Long, List<Equipo>> equiposPorSala = equipoRepository.findAllConSala()
                .stream()
                .collect(Collectors.groupingBy(equipo -> equipo.getSala().getId()));

        return salaRepository.findAll().stream()
                .map(sala -> {
                    List<Verificacion> verificaciones =
                            porSala.getOrDefault(sala.getId(), List.of());
                    List<Equipo> equipos =
                            equiposPorSala.getOrDefault(sala.getId(), List.of());

                    int operativos = 0;
                    int enMantenimiento = 0;
                    int fueraDeServicio = 0;

                    for (Equipo equipo : equipos) {
                        String estado = equipo.getEstado();
                        if (OPERATIVO.equals(estado)) {
                            operativos++;
                        } else if (EN_MANTENIMIENTO.equals(estado)) {
                            enMantenimiento++;
                        } else if (FUERA_DE_SERVICIO.equals(estado)) {
                            fueraDeServicio++;
                        }
                    }

                    return new VerificacionSalaResponse(
                            sala.getId(),
                            sala.getNombre(),
                            sala.getCapacidadMaxima(),
                            equipos.size(),
                            operativos,
                            enMantenimiento,
                            fueraDeServicio,
                            verificaciones,
                            verificaciones.isEmpty() ? null : verificaciones.get(0),
                            equipos
                    );
                })
                .collect(Collectors.toList());
    }

    /** Historial de conteos de una sala concreta. */
    @Transactional(readOnly = true)
    public List<Verificacion> obtenerPorSala(Long idSala) {
        return verificacionRepository.findBySalaIdConDetalles(idSala);
    }

    /** Historial completo de conteos de la institución. */
    @Transactional(readOnly = true)
    public List<Verificacion> obtenerTodas() {
        return verificacionRepository.findAllConDetalles();
    }

    /** Suma de todos los conteos registrados; alimenta el resumen del administrador. */
    @Transactional(readOnly = true)
    public long totalVerificaciones() {
        return verificacionRepository.count();
    }

    /**
     * Usuario que hizo el recorrido.
     *
     * Va por correo porque es lo que deja el filtro JWT como principal. Si no
     * se encuentra (token de una cuenta ya borrada), el conteo se guarda igual
     * y sin autor: perder el número de equipos contados sería peor que perder
     * el nombre de quién los contó.
     */
    private Usuario usuarioDelCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }

        return usuarioRepository.findByCorreo(correo).orElse(null);
    }
}
