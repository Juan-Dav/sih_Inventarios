package com.sih_inventarios_backend.dto;

import java.time.LocalDateTime;

/**
 * Cómo está una sala: lo declarado, lo registrado y lo que hay en cada estado.
 *
 * Es una fila del módulo de inventario general y de las tablas de conteo que
 * pide el administrador ("cuántos equipos hay en cada sala") y el técnico
 * ("verificar físicamente la cantidad de equipos de cada sala").
 */
public record ConteoSalaResponse(
        Long salaId,
        String sala,
        int capacidadMaxima,
        int equiposRegistrados,
        int equiposOperativos,
        int equiposEnMantenimiento,
        int equiposFueraDeServicio,
        /** Fecha del último conteo físico, o `null` si la sala nunca se ha recorrido. */
        LocalDateTime ultimaVerificacion,
        /** Diferencia del último conteo físico. `null` si nunca se ha recorrido. */
        Integer equiposFaltantes
) {
}
