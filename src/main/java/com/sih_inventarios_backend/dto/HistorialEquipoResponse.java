package com.sih_inventarios_backend.dto;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.entity.Mantenimiento;

import java.util.List;

/**
 * Historial completo de un equipo: sus fallas y sus mantenimientos.
 *
 * Se devuelve junto al equipo para que el frontend pueda pintar la ficha sin
 * tener que buscarlo en otra lista.
 */
public record HistorialEquipoResponse(
        Equipo equipo,
        List<Falla> fallas,
        List<Mantenimiento> mantenimientos
) {
}