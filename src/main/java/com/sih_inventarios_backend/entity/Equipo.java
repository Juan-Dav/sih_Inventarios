package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "equipos")
@Data
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código del equipo es obligatorio")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo; // Ej: PC-001, PC-002

    @Column(length = 150)
    private String nombreEquipo;

    @Column(length = 80)
    private String categoria;

    @Column(length = 200)
    private String caracteristicas; // Ej: "Procesador Intel i5, 8GB RAM, Disco 500GB"

    /** Marca del equipo, para las características principales. Ej: "Dell", "HP". */
    @Column(length = 60)
    private String marca;

    /** Modelo del equipo. Ej: "OptiPlex 3080", "LaserJet M428". */
    @Column(length = 60)
    private String modelo;

    @Column(length = 100)
    private String numeroSerie;

    private LocalDate fechaAdquisicion;

    private LocalDate garantiaHasta;

    @Column(length = 150)
    private String ubicacionDetalle;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(unique = true, length = 100)
    private String codigoQr;

    @Column(length = 255)
    private String rutaQr;

    @Column(nullable = false, length = 50)
    private String estado = "OPERATIVO"; // Puede ser: OPERATIVO, EN_MANTENIMIENTO, FUERA_DE_SERVICIO

    // Relación: Muchos equipos están en una sola sala (ubicación física)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_custodia_id")
    private ResponsableCustodia responsableCustodia;
}
