package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_custodia")
@Data
public class HistorialCustodia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_anterior_id")
    private Area areaAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departamento_anterior_id")
    private Departamento departamentoAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_anterior_id")
    private ResponsableCustodia responsableAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_nueva_id")
    private Area areaNueva;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departamento_nuevo_id")
    private Departamento departamentoNuevo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_nuevo_id")
    private ResponsableCustodia responsableNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_que_asigna_id")
    private Usuario usuarioQueAsigna;

    @Column(length = 500)
    private String observaciones;
}
