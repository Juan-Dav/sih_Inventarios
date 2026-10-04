package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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

    @Column(length = 200)
    private String caracteristicas; // Ej: "Procesador Intel i5, 8GB RAM, Disco 500GB"

    /** Marca del equipo, para las características principales. Ej: "Dell", "HP". */
    @Column(length = 60)
    private String marca;

    /** Modelo del equipo. Ej: "OptiPlex 3080", "LaserJet M428". */
    @Column(length = 60)
    private String modelo;

    @Column(nullable = false, length = 50)
    private String estado = "OPERATIVO"; // Puede ser: OPERATIVO, EN_MANTENIMIENTO, FUERA_DE_SERVICIO

    // Relación: Muchos equipos están en una sola sala
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;
}