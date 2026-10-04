package com.sih_inventarios_backend.dto;

import java.time.LocalDateTime;

/**
 * Estado del plan con el consumo actual incluido.
 *
 * Se manda todo junto para que el frontend pueda pintar la barra de uso y el
 * aviso de límite en una sola llamada, sin tener que cruzarse con el conteo
 * de salas y equipos.
 */
public record PlanResponse(
        String plan,
        long precio,
        Integer maxSalas,
        Integer maxEquiposPorSala,
        /** `true` cuando el plan no pone tope de salas. Evita que el frontend tenga que interpretar el `null`. */
        boolean salasIlimitadas,
        /** `true` cuando el plan no pone tope de equipos por sala. */
        boolean equiposIlimitados,
        String estadoPago,
        String referenciaPago,
        LocalDateTime fechaActivacion,
        int salasRegistradas,
        int equiposRegistrados
) {
}