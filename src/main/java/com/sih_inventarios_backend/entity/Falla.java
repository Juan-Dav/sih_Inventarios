package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "fallas")
@Data
public class Falla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String codigoReporte;

    @NotBlank(message = "La descripción de la falla es obligatoria")
    @Column(nullable = false, length = 500)
    private String descripcion; // Ej: "El PC no enciende y huele a quemado"

    @Column(nullable = false, length = 50)
    private String estado = "PENDIENTE"; // PENDIENTE, EN_REVISION, RESUELTO

    @Column(length = 20)
    private String prioridad = "MEDIA"; // BAJA, MEDIA, ALTA, CRITICA

    // Guarda automáticamente la fecha y hora del reporte
    @Column(nullable = false)
    private LocalDateTime fechaReporte = LocalDateTime.now();

    private LocalDateTime fechaAsignacion;

    private LocalDateTime fechaResolucion;

    @Column(columnDefinition = "TEXT")
    private String accionesRealizadas;

    @Column(columnDefinition = "TEXT")
    private String observacionesSeguimiento;

    // Relación: Una falla le pertenece a un equipo específico
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reportado_por_id")
    private Usuario reportadoPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asignado_a_id")
    private Usuario asignadoA;
}
