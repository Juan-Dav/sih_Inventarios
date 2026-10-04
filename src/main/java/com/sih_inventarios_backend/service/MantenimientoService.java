package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.entity.Mantenimiento;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoRepository equipoRepository;
    private final FallaRepository fallaRepository;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
                                EquipoRepository equipoRepository,
                                FallaRepository fallaRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoRepository = equipoRepository;
        this.fallaRepository = fallaRepository;
    }

    @Transactional
    public Mantenimiento registrarMantenimiento(Mantenimiento mantenimiento, Long idEquipo, Long idFalla) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(idEquipo);
        Optional<Falla> fallaOpt = fallaRepository.findById(idFalla);

        if (equipoOpt.isEmpty() || fallaOpt.isEmpty()) {
            throw new RuntimeException("Error: El equipo o la falla no existen.");
        }

        Equipo equipo = equipoOpt.get();
        Falla falla = fallaOpt.get();

        // 0. La falla debe pertenecer al equipo que se está reparando
        if (falla.getEquipo() == null || !falla.getEquipo().getId().equals(idEquipo)) {
            throw new RuntimeException("Error: La falla indicada no pertenece al equipo indicado.");
        }

        if ("RESUELTO".equals(falla.getEstado())) {
            throw new RuntimeException("Error: La falla ya fue resuelta anteriormente.");
        }

        // 1. Actualizamos la falla a RESUELTO
        falla.setEstado("RESUELTO");
        fallaRepository.save(falla);

        // 2. Actualizamos el equipo a OPERATIVO nuevamente
        equipo.setEstado("OPERATIVO");
        equipoRepository.save(equipo);

        // 3. Guardamos el mantenimiento
        mantenimiento.setEquipo(equipo);
        mantenimiento.setFalla(falla);
        return mantenimientoRepository.save(mantenimiento);
    }

    public List<Mantenimiento> obtenerTodos() {
        return mantenimientoRepository.findAllByOrderByFechaMantenimientoDesc();
    }

    /**
     * Mantenimientos de un equipo, del más reciente al más antiguo.
     *
     * Es el "historial de mantenimiento y fallas de cada equipo" que consulta el
     * técnico antes de intervenir.
     */
    @Transactional(readOnly = true)
    public List<Mantenimiento> obtenerPorEquipo(Long idEquipo) {
        return mantenimientoRepository.findByEquipoIdOrderByFechaMantenimientoDesc(idEquipo);
    }
}