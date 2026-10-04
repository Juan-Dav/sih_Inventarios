package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Plan contratado por la institución.
 *
 * Se guarda una sola fila: es el estado del software, no algo que se cree por
 * cada usuario. Por eso el `id` es fijo (1) y no autogenerado.
 *
 * Los límites van en `Integer` y no en `int` a propósito: `null` significa
 * "sin límite", que es justo lo del plan Premium. Con `int` no habría forma de
 * distinguir "cero permitidos" de "ilimitado".
 */
@Entity
@Table(name = "plan_institucion")
@Data
public class Plan {

    public static final String GRATUITO = "GRATUITO";
    public static final String PREMIUM = "PREMIUM";

    /** Estados posibles de `estadoPago`. */
    public static final String PAGO_PENDIENTE = "PENDIENTE";
    public static final String PAGO_PAGADO = "PAGADO";

    /**
     * Se guardó en vez de volver a PENDIENTE para que quede registro de que hubo
     * una compra y se terminó, aunque el nombre del plan ya sea el gratuito.
     */
    public static final String PAGO_CANCELADO = "CANCELADO";

    public static final long PRECIO_PREMIUM = 150_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** GRATUITO o PREMIUM. */
    @Column(nullable = false, length = 20)
    private String nombre = GRATUITO;

    /** Valor en pesos. 0 para el plan gratuito. */
    @Column(nullable = false)
    private long precio = 0L;

    /** Máximo de salas que se pueden registrar. `null` = ilimitado. */
    private Integer maxSalas;

    /** Máximo de equipos por sala. `null` = ilimitado. */
    private Integer maxEquiposPorSala;

    /** PENDIENTE mientras la compra no se confirma; PAGADO cuando queda activa. */
    @Column(nullable = false, length = 20)
    private String estadoPago = PAGO_PENDIENTE;

    /** Comprobante generado al comprar. */
    @Column(length = 40)
    private String referenciaPago;

    private LocalDateTime fechaActivacion;
}