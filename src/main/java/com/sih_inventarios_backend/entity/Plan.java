package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Plan contratado por la institución.
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
    public static final String PAGO_CANCELADO = "CANCELADO";
    public static final String PAGO_VENCIDO = "VENCIDO";

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

    @Column(length = 3)
    private String moneda = "COP";

    @Column(length = 20)
    private String periodicidad = "MENSUAL";

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

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaVencimiento;

    @Column(nullable = false)
    private Boolean renovacionAutomatica = false;
}
