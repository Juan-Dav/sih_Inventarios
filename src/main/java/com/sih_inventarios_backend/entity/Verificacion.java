package com.sih_inventarios_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Verificación física de los equipos de una sala.
 *
 * Es el registro que hace el técnico cuando recorre un aula y cuenta lo que
 * hay realmente, para compararlo con lo que dice el inventario. De ahí sale la
 * detección de equipos faltantes: la diferencia entre lo esperado (lo
 * registrado) y lo encontrado.
 *
 * `cantidadEsperada` no se toma del formulario: la calcula el backend con los
 * equipos registrados en la sala, porque si la enviara el cliente un técnico
 * podría "verificar" un número falso y el conteo no serviría para nada.
 */
@Entity
@Table(name = "verificaciones")
@Data
public class Verificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Equipos que el inventario dice que hay en la sala. Lo calcula el backend. */
    @Column(nullable = false)
    private int cantidadEsperada;

    /** Equipos que el técnico contó a mano en la sala. */
    @Column(nullable = false)
    private int cantidadEncontrada;

    /** Códigos de los equipos que no aparecen o no se pudieron identificar. */
    @Column(length = 500)
    private String equiposFaltantes;

    /**
     * Códigos de los equipos que sí están pero ya no sirven.
     *
     * Es lo que el técnico ve en el recorrido y no se puede adivinar desde el
     * conteo: un aula puede tener sus doce equipos y aun así tener dos quemados.
     * Se guardan como texto, igual que los faltantes, porque al registrar el
     * conteo no se decide el estado de cada equipo: eso es una operación aparte,
     * con su propio registro de quién la hizo.
     */
    @Column(length = 500)
    private String equiposDanados;

    /** Daños u otras novedades vistas durante el recorrido. */
    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false)
    private LocalDateTime fechaVerificacion = LocalDateTime.now();

    // Relación: la verificación se hace sobre una sala concreta
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    // Relación opcional: quién hizo el recorrido
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    /**
     * Diferencia entre lo esperado y lo encontrado.
     *
     * Va como método y no como columna: se deriva de los dos números que ya
     * se guardan, así que no puede quedar desincronizada.
     *
     * Nunca sale negativa. Puede pasar que el técnico cuente un equipo que no
     * está registrado en la sala, y ese exceso es un problema del inventario,
     * no equipos faltantes: reportarlo como "-2" se leería como que no falta
     * nada. El sobrante se detecta en `cantidadEncontrada`.
     */
    public int getCantidadFaltante() {
        return Math.max(0, cantidadEsperada - cantidadEncontrada);
    }

    /** `true` cuando el conteo físico coincide con el inventario. */
    public boolean isCompleta() {
        return getCantidadFaltante() == 0;
    }
}
