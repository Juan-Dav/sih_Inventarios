package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.dto.ConteoSalaResponse;
import com.sih_inventarios_backend.dto.ResumenInventarioResponse;
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
import com.sih_inventarios_backend.repository.VerificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lectura consolidada del inventario.
 *
 * Existe para que el administrador pueda "consultar el inventario general, la
 * cantidad de equipos de cada sala, el estado actual de los equipos, los que
 * están fuera de servicio, las fallas y los mantenimientos" sin que el panel
 * tenga que cruzarse cinco listados por su cuenta. También lo usa el técnico
 * para el seguimiento de los equipos fuera de servicio.
 */
@Service
public class InventarioService {

    private static final String OPERATIVO = "OPERATIVO";
    private static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO";
    private static final String FUERA_DE_SERVICIO = "FUERA_DE_SERVICIO";

    private static final String FALLA_PENDIENTE = "PENDIENTE";
    private static final String FALLA_EN_REVISION = "EN_REVISION";
    private static final String FALLA_RESUELTO = "RESUELTO";

    private final SalaRepository salaRepository;
    private final EquipoRepository equipoRepository;
    private final FallaRepository fallaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final VerificacionRepository verificacionRepository;

    public InventarioService(
            SalaRepository salaRepository,
            EquipoRepository equipoRepository,
            FallaRepository fallaRepository,
            MantenimientoRepository mantenimientoRepository,
            VerificacionRepository verificacionRepository) {
        this.salaRepository = salaRepository;
        this.equipoRepository = equipoRepository;
        this.fallaRepository = fallaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.verificacionRepository = verificacionRepository;
    }

    /** Fotografía completa del inventario. */
    @Transactional(readOnly = true)
    public ResumenInventarioResponse obtenerResumen() {
        List<Sala> salas = salaRepository.findAll();
        Map<Long, VerificacionPorSala> ultimoConteo = ultimasVerificaciones();

        List<ConteoSalaResponse> conteos = salas.stream()
                .map(sala -> {
                    VerificacionPorSala conteo = ultimoConteo.get(sala.getId());

                    return new ConteoSalaResponse(
                            sala.getId(),
                            sala.getNombre(),
                            sala.getCapacidadMaxima(),
                            (int) equipoRepository.countBySalaId(sala.getId()),
                            (int) equipoRepository.countBySalaIdAndEstado(sala.getId(), OPERATIVO),
                            (int) equipoRepository.countBySalaIdAndEstado(sala.getId(), EN_MANTENIMIENTO),
                            (int) equipoRepository.countBySalaIdAndEstado(sala.getId(), FUERA_DE_SERVICIO),
                            conteo == null ? null : conteo.fecha(),
                            conteo == null ? null : conteo.faltantes()
                    );
                })
                .collect(Collectors.toList());

        return new ResumenInventarioResponse(
                salas.size(),
                (int) equipoRepository.count(),
                salas.stream().mapToInt(Sala::getCapacidadMaxima).sum(),
                (int) equipoRepository.countByEstado(OPERATIVO),
                (int) equipoRepository.countByEstado(EN_MANTENIMIENTO),
                (int) equipoRepository.countByEstado(FUERA_DE_SERVICIO),
                (int) fallaRepository.count(),
                (int) fallaRepository.countByEstado(FALLA_PENDIENTE),
                (int) fallaRepository.countByEstado(FALLA_EN_REVISION),
                (int) fallaRepository.countByEstado(FALLA_RESUELTO),
                (int) mantenimientoRepository.count(),
                (int) verificacionRepository.count(),
                conteos,
                equipoRepository.findByEstadoOrderByCodigoAsc(FUERA_DE_SERVICIO),
                equipoRepository.findByEstadoOrderByCodigoAsc(EN_MANTENIMIENTO),
                fallaRepository.findByEstadoNotOrderByFechaReporteAsc(FALLA_RESUELTO)
        );
    }

    /**
     * Último conteo físico de cada sala.
     *
     * Se agrupa en memoria con la lista ya ordenada de más nueva a más vieja,
     * así que el primero de cada grupo es el último conteo. Se hace así y no
     * con una consulta por sala porque son varias y el panel se abre entero de
     * una vez.
     */
    private Map<Long, VerificacionPorSala> ultimasVerificaciones() {
        return verificacionRepository.findAllByOrderByFechaVerificacionDesc().stream()
                .collect(Collectors.toMap(
                        verificacion -> verificacion.getSala().getId(),
                        verificacion -> new VerificacionPorSala(
                                verificacion.getFechaVerificacion(),
                                verificacion.getCantidadFaltante()),
                        // La lista viene ordenada, así que el primer insertado gana.
                        (primero, ultimo) -> primero
                ));
    }

    /** Fecha y diferencia del último conteo de una sala. */
    private record VerificacionPorSala(java.time.LocalDateTime fecha, int faltantes) {
    }
}
