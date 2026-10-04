package com.sih_inventarios_backend.dto;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Verificacion;

import java.util.List;

/**
 * Sala con su estado real: lo que dice el inventario y lo que se contó a mano.
 *
 * Es lo que el técnico necesita para "verificar físicamente la cantidad de
 * equipos existentes en cada sala" y "detectar equipos faltantes, dañados o
 * fuera de servicio" sin tener que recorrer cinco peticiones.
 */
public record VerificacionSalaResponse(
        Long salaId,
        String sala,
        int capacidadMaxima,
        int equiposRegistrados,
        int equiposOperativos,
        int equiposEnMantenimiento,
        int equiposFueraDeServicio,
        List<Verificacion> verificaciones,
        Verificacion ultimaVerificacion,
        List<Equipo> equipos
) {

    /** Diferencia del último conteo, o `null` si la sala nunca se ha recorrido. */
    public Integer getEquiposFaltantes() {
        return ultimaVerificacion == null ? null : ultimaVerificacion.getCantidadFaltante();
    }

    /** `true` cuando ya se hizo al menos un conteo físico en la sala. */
    public boolean isVerificada() {
        return ultimaVerificacion != null;
    }
}
