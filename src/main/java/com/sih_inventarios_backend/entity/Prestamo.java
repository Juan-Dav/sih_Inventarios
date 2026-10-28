package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestamos")
@Data
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_custodia_id")
    private ResponsableCustodia responsable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_prestamo_id", nullable = false)
    private Usuario usuarioPrestamo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_entrega_id")
    private Usuario usuarioEntrega;

    @Column(nullable = false)
    private LocalDateTime fechaSalida = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime fechaDevolucionEstimada;

    private LocalDateTime fechaDevolucionReal;

    @Column(nullable = false, length = 20)
    private String estado = "PRESTADO"; // PRESTADO, DEVUELTO, VENCIDO, CANCELADO

    @Column(length = 500)
    private String observaciones;

    @Column(length = 500)
    private String observacionesDevolucion;
}
