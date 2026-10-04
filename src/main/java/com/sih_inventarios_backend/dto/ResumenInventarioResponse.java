package com.sih_inventarios_backend.dto;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;

import java.util.List;

/**
 * Fotografía completa del inventario en una sola llamada.
 *
 * El administrador necesita "consultar el inventario general, la cantidad de
 * equipos por sala, el estado actual y los que están fuera de servicio, las
 * fallas y los mantenimientos". Pedir cada cosa por separado le obligaría a
 * cruzar cinco listados en el navegador; aquí ya viene todo junto y el panel
 * solo pinta.
 */
public record ResumenInventarioResponse(
        int totalSalas,
        int totalEquipos,
        int capacidadTotal,
        int equiposOperativos,
        int equiposEnMantenimiento,
        int equiposFueraDeServicio,
        int totalFallas,
        int fallasPendientes,
        int fallasEnRevision,
        int fallasResueltas,
        int totalMantenimientos,
        int totalVerificaciones,
        List<ConteoSalaResponse> salas,
        // Los conteos de arriba van en número; estas listas traen los equipos uno
        // a uno, para poder abrir su ficha sin volver a pedirlos.
        List<Equipo> listaEquiposFueraDeServicio,
        List<Equipo> listaEquiposEnMantenimiento,
        /** Fallas que siguen abiertas, de la más antigua a la más reciente. */
        List<Falla> fallasAbiertas
) {
}
