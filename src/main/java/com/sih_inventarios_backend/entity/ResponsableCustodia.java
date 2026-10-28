package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "responsables_custodia")
@Data
public class ResponsableCustodia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 100)
    private String cargo;

    @Column(length = 50)
    private String tipoResponsable; // DOCENTE, TECNICO, ADMINISTRADOR, OTRO

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean activo = true;
}
