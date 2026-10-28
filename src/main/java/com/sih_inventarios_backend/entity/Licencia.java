package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "licencias")
@Data
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreSoftware;

    @Column(length = 100)
    private String fabricante;

    @Column(length = 50)
    private String tipoLicencia;

    @Column(nullable = false)
    private Integer cantidadAdquiridas;

    @Column(nullable = false)
    private Integer cantidadInstaladas = 0;

    private LocalDate fechaAdquisicion;

    private LocalDate fechaVencimiento;

    @Column(length = 20)
    private String estado = "ACTIVA";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String claveLicencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    private ResponsableCustodia responsable;

    @Column(length = 1000)
    private String observaciones;
}
