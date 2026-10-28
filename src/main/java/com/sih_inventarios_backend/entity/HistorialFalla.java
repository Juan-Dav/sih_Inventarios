package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_fallas")
@Data
public class HistorialFalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "falla_id", nullable = false)
    private Falla falla;

    @Column(length = 50)
    private String estadoAnterior;

    @Column(length = 50)
    private String estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_cambio_id")
    private Usuario usuarioCambio;

    @Column(length = 1000)
    private String comentario;
}
