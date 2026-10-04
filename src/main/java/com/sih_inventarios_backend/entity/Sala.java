package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "salas")
@Data
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la sala es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre; // Ej: Laboratorio de Sistemas 1

    @Min(value = 1, message = "La capacidad máxima debe ser mayor a 0")
    @Column(nullable = false)
    private int capacidadMaxima = 12; // Aquí controlaremos el límite de los 12 equipos que definiste
}