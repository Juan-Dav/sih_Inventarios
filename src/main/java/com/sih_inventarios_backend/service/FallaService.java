package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FallaService {

    private static final String RESUELTO = "RESUELTO";
    private static final String FUERA_DE_SERVICIO = "FUERA_DE_SERVICIO";

    private final FallaRepository fallaRepository;

    private final EquipoRepository equipoRepository;

    public FallaService(FallaRepository fallaRepository, EquipoRepository equipoRepository) {
        this.fallaRepository = fallaRepository;
        this.equipoRepository = equipoRepository;
    }

    /**
     * Da de alta una falla.
     *
     * La reportan tanto el docente (cuando encuentra una novedad) como el
     * técnico (cuando la detecta en el recorrido o en una falla ya resuelta a
     * medias), así que no se guarda quién la registró: el registro es de la
     * institución, no de una persona.
     */
    @Transactional
    public Falla reportarFalla(Falla falla, Long idEquipo) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(idEquipo);
        if (equipoOpt.isEmpty()) {
            throw new RuntimeException("Error: El equipo no existe.");
        }

        Equipo equipo = equipoOpt.get();

        // El estado del equipo se deriva de la falla: una máquina con una falla
        // abierta no puede figurar como operativa, o el inventario daría una
        // disponibilidad que no es real.
        equipo.setEstado(FUERA_DE_SERVICIO);
        equipoRepository.save(equipo);

        falla.setEquipo(equipo);
        falla.setEstado(validarEstado(falla.getEstado()));
        falla.setFechaReporte(java.time.LocalDateTime.now());
        return fallaRepository.save(falla);
    }

    /** Todas las fallas, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<Falla> obtenerTodas() {
        return fallaRepository.findAll();
    }

    /**
     * Fallas que siguen abiertas: la cola de trabajo del técnico y lo que el
     * docente revisa cuando consulta el estado de lo que reportó.
     */
    @Transactional(readOnly = true)
    public List<Falla> obtenerAbiertas() {
        return fallaRepository.findByEstadoNotOrderByFechaReporteAsc(RESUELTO);
    }

    /** Fallas filtradas por estado, para las tablas del panel. */
    @Transactional(readOnly = true)
    public List<Falla> obtenerPorEstado(String estado) {
        return fallaRepository.findByEstadoOrderByFechaReporteDesc(validarEstado(estado));
    }

    /**
     * Cambia el estado de una falla.
     *
     * Sin esto una falla se quedaria en PENDIENTE para siempre aunque el técnico
     * ya la atendiera, porque el backend solo tenia alta y listado.
     */
    @Transactional
    public Falla actualizarEstado(Long id, String estado) {
        Falla falla = fallaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: La falla no existe."));

        falla.setEstado(validarEstado(estado));
        return fallaRepository.save(falla);
    }

    /** Normaliza y valida el estado de una falla. */
    private String validarEstado(String estado) {
        String limpio = estado == null || estado.isBlank() ? "PENDIENTE" : estado.trim().toUpperCase();
        if (!ESTADOS.contains(limpio)) {
            throw new RuntimeException(
                    "Error: Estado no válido. Usa PENDIENTE, EN_REVISION o RESUELTO.");
        }
        return limpio;
    }

    /** Estados válidos de una falla. */
    private static final List<String> ESTADOS = List.of("PENDIENTE", "EN_REVISION", RESUELTO);
}
