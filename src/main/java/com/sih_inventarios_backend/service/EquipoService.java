package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.dto.HistorialEquipoResponse;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;

    private final SalaRepository salaRepository;

    private final PlanService planService;

    private final FallaRepository fallaRepository;

    private final MantenimientoRepository mantenimientoRepository;

    public EquipoService(
            EquipoRepository equipoRepository,
            SalaRepository salaRepository,
            PlanService planService,
            FallaRepository fallaRepository,
            MantenimientoRepository mantenimientoRepository) {
        this.equipoRepository = equipoRepository;
        this.salaRepository = salaRepository;
        this.planService = planService;
        this.fallaRepository = fallaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    /** Fallas y mantenimientos de un equipo, para la pantalla de historial. */
    public HistorialEquipoResponse obtenerHistorial(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El equipo no existe."));

        return new HistorialEquipoResponse(
                equipo,
                fallaRepository.findByEquipoIdOrderByFechaReporteDesc(id),
                mantenimientoRepository.findByEquipoIdOrderByFechaMantenimientoDesc(id)
        );
    }

    /** Equipos en un estado concreto; el backend normaliza y valida el estado. */
    @Transactional(readOnly = true)
    public List<Equipo> obtenerPorEstado(String estado) {
        return equipoRepository.findByEstadoOrderByCodigoAsc(validarEstado(estado));
    }

    /**
     * Equipos fuera de servicio.
     *
     * Es lo que piden el administrador ("consultar los equipos que se
     * encuentran fuera de servicio") y el técnico ("realizar seguimiento a los
     * equipos que se encuentren fuera de servicio"): los dos entran por aquí.
     */
    @Transactional(readOnly = true)
    public List<Equipo> obtenerFueraDeServicio() {
        return equipoRepository.findByEstadoOrderByCodigoAsc(FUERA_DE_SERVICIO);
    }

    @Transactional
    public Equipo registrarEquipo(Equipo equipo, Long idSala) {
        // 1. Buscamos que la sala exista
        Optional<Sala> salaOpcional = salaRepository.findById(idSala);

        if (salaOpcional.isEmpty()) {
            throw new RuntimeException("Error: La sala especificada no existe.");
        }

        Sala sala = salaOpcional.get();

        // 2. Tope del plan: en el gratuito no se pueden pasar de 12 equipos por
        //    sala, aunque la sala admita más.
        planService.verificarLimiteEquiposPorSala(idSala);

        // 3. Tope declarado por la propia sala
        List<Equipo> equiposEnSala = equipoRepository.findBySalaId(idSala);
        if (equiposEnSala.size() >= sala.getCapacidadMaxima()) {
            throw new RuntimeException("Límite alcanzado: La sala \"" + sala.getNombre()
                    + "\" ya tiene el máximo de " + sala.getCapacidadMaxima() + " equipos permitidos.");
        }

        // 4. Asignamos la sala al equipo y lo guardamos
        equipo.setSala(sala);
        // El estado se valida también en el alta: si no, un valor inventado
        // entraría en la base y ninguna de las consultas por estado lo vería.
        equipo.setEstado(validarEstado(equipo.getEstado()));
        return equipoRepository.save(equipo);
    }

    /** Cambia el código, las características o la sala de un equipo. */
    @Transactional
    public Equipo actualizarEquipo(Long id, Equipo cambios, Long idSala) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El equipo no existe."));

        if (cambios.getCodigo() != null && !cambios.getCodigo().isBlank()) {
            equipo.setCodigo(cambios.getCodigo().trim());
        }

        equipo.setCaracteristicas(cambios.getCaracteristicas());
        equipo.setMarca(cambios.getMarca());
        equipo.setModelo(cambios.getModelo());

        if (cambios.getEstado() != null && !cambios.getEstado().isBlank()) {
            equipo.setEstado(validarEstado(cambios.getEstado()));
        }

        // Trasladar el equipo a otra sala cuenta como equipo nuevo en esa sala:
        // hay que comprobar los dos topes antes de moverlo.
        if (idSala != null && !idSala.equals(equipo.getSala().getId())) {
            Optional<Sala> destino = salaRepository.findById(idSala);
            if (destino.isEmpty()) {
                throw new RuntimeException("Error: La sala especificada no existe.");
            }

            Sala nueva = destino.get();
            planService.verificarLimiteEquiposPorSala(nueva.getId());

            if (equipoRepository.findBySalaId(nueva.getId()).size() >= nueva.getCapacidadMaxima()) {
                throw new RuntimeException("Límite alcanzado: La sala \"" + nueva.getNombre()
                        + "\" ya tiene el máximo de " + nueva.getCapacidadMaxima() + " equipos permitidos.");
            }

            equipo.setSala(nueva);
        }

        return equipoRepository.save(equipo);
    }

    public List<Equipo> obtenerTodos() {
        return equipoRepository.findAll();
    }

    /** Equipos de una sala, para el conteo por aula y la verificación física. */
    @Transactional(readOnly = true)
    public List<Equipo> obtenerPorSala(Long idSala) {
        return equipoRepository.findBySalaId(idSala);
    }

    /**
     * Cambia el estado de un equipo.
     *
     * Al reportarse una falla el equipo pasa solo a FUERA_DE_SERVICIO; hace
     * falta poder devolverlo a OPERATIVO cuando el técnico lo deja
     * funcionando, y por eso se añadió este método.
     */
    @Transactional
    public Equipo actualizarEstado(Long id, String estado) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El equipo no existe."));

        equipo.setEstado(validarEstado(estado));
        return equipoRepository.save(equipo);
    }

    /**
     * Normaliza y valida un estado de equipo.
     *
     * Va en un método aparte porque el mismo estado se valida en tres sitios
     * (alta, edición y cambio puntual) y repetir la lista de valores válidos en
     * cada uno terminaría dejando alguno sin comprobar.
     */
    private String validarEstado(String estado) {
        String limpio = estado == null ? "" : estado.trim().toUpperCase();
        if (!ESTADOS.contains(limpio)) {
            throw new RuntimeException(
                    "Error: Estado no válido. Usa OPERATIVO, EN_MANTENIMIENTO o FUERA_DE_SERVICIO.");
        }
        return limpio;
    }

    /** Estados válidos de un equipo. */
    private static final String FUERA_DE_SERVICIO = "FUERA_DE_SERVICIO";

    private static final List<String> ESTADOS =
            List.of("OPERATIVO", "EN_MANTENIMIENTO", FUERA_DE_SERVICIO);
}