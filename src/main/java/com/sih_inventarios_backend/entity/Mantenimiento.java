package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimientos")
@Data
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Column(nullable = false, length = 500)
    private String diagnostico; // Ej: "Fuente de poder quemada por bajón de luz"

    @NotBlank(message = "La solución es obligatoria")
    @Column(nullable = false, length = 500)
    private String solucion; // Ej: "Se reemplazó la fuente de poder por una nueva de 500W"

    @Column(nullable = false)
    private LocalDateTime fechaMantenimiento = LocalDateTime.now();

    // Relación: El mantenimiento se le hace a un equipo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // Relación opcional: El mantenimiento resuelve una falla específica
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "falla_id")
    private Falla falla;
}