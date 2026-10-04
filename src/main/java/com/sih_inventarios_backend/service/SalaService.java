package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.SalaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salaRepository;

    private final PlanService planService;

    public SalaService(SalaRepository salaRepository, PlanService planService) {
        this.salaRepository = salaRepository;
        this.planService = planService;
    }

    /**
     * Registra una nueva sala.
     *
     * Antes de guardar, {@link PlanService} revisa si el plan actual ya tiene
     * el máximo de salas: en el gratuito solo se permite una.
     */
    public Sala guardarSala(Sala sala) {
        planService.verificarLimiteSalas();
        return salaRepository.save(sala);
    }

    /** Cambia el nombre o la capacidad declarada de una sala existente. */
    public Sala actualizarSala(Long id, Sala cambios) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: La sala no existe."));

        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
            sala.setNombre(cambios.getNombre().trim());
        }

        if (cambios.getCapacidadMaxima() > 0) {
            // La capacidad es el tope declarado por el aula. Puede bajar por
            // debajo de los equipos ya registrados si alguien la reduce, así que
            // se avisa en vez de fallar: el inventario real manda.
            sala.setCapacidadMaxima(cambios.getCapacidadMaxima());
        }

        return salaRepository.save(sala);
    }

    public List<Sala> obtenerTodas() {
        return salaRepository.findAll();
    }
}