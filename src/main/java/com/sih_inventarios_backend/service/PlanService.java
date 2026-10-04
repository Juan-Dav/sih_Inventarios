package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.dto.PlanResponse;
import com.sih_inventarios_backend.entity.Plan;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.PlanRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Gestor del plan de la institución.
 *
 * Hay dos formas de pagar:
 *
 * - Gratuita: dos salas y 12 equipos como máximo dentro de cada una. Sirve para
 *   demostrar el sistema con una instalación real pequeña.
 * - Premium: 150.000 pesos, y se compra desde el propio software. Quita los dos
 *   topes, así que la institución puede registrar tantas aulas y tantos equipos
 *   por aula como necesite.
 *
 * La compra es simulada: no hay pasarela ni cobro real, solo se marca el plan
 * como pagado y queda activo al instante. También se puede cancelar desde
 * cualquier panel, y al hacerlo vuelven los topes del gratuito sin borrar nada
 * de lo que ya está registrado.
 *
 * Los límites del plan gratuito salen de `application.properties` y no de
 * números escritos aquí, para poder cambiar la demostración sin tocar código.
 */
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final SalaRepository salaRepository;
    private final EquipoRepository equipoRepository;

    /** Salas máximas del plan gratuito. */
    private final int maxSalasGratuito;

    /** Equipos máximos por sala en el plan gratuito. */
    private final int maxEquiposGratuito;

    /** Precio del plan Premium, en pesos. */
    private final long precioPremium;

    public PlanService(
            PlanRepository planRepository,
            SalaRepository salaRepository,
            EquipoRepository equipoRepository,
            @Value("${app.plan.gratuito.max-salas:2}") int maxSalasGratuito,
            @Value("${app.plan.gratuito.max-equipos-por-sala:12}") int maxEquiposGratuito,
            @Value("${app.plan.premium.precio:150000}") long precioPremium) {
        this.planRepository = planRepository;
        this.salaRepository = salaRepository;
        this.equipoRepository = equipoRepository;
        this.maxSalasGratuito = maxSalasGratuito;
        this.maxEquiposGratuito = maxEquiposGratuito;
        this.precioPremium = precioPremium;
    }

    /**
     * Devuelve el plan activo, creándolo con valores gratuitos si no existe.
     *
     * Se busca la primera fila en vez de fijar un id a mano: si se asignara el
     * id antes de guardar, `save()` haría un `merge` de una fila que todavía no
     * existe y Hibernate lanzaría StaleObjectStateException.
     */
    @Transactional
    public Plan obtenerPlanActivo() {
        Plan plan = planRepository.findFirstByOrderByIdAsc().orElseGet(this::crearPlanGratis);
        return alinearLimitesGratuito(plan);
    }

    /**
     * Copia los topes de la configuración al plan gratuito guardado.
     *
     * Los límites del gratuito salen de `application.properties`, pero lo que
     * valida de verdad son los de la fila de `plan_institucion`, y esa fila se
     * creó una sola vez: si alguien cambia `max-salas` en el archivo y la base
     * ya tenía el plan de antes, el sistema seguiría aplicando el número viejo
     * sin avisar. Por eso se sincroniza en cada lectura.
     *
     * Solo se escribe cuando los números no coinciden, así que en el caso
     * normal no hay ni un UPDATE extra. Y nunca se toca el Premium, que no
     * lleva topes: sus `null` son justamente lo que lo distingue del gratuito.
     */
    private Plan alinearLimitesGratuito(Plan plan) {
        if (!Plan.GRATUITO.equals(plan.getNombre())) {
            return plan;
        }

        if (Objects.equals(plan.getMaxSalas(), maxSalasGratuito)
                && Objects.equals(plan.getMaxEquiposPorSala(), maxEquiposGratuito)) {
            return plan;
        }

        plan.setMaxSalas(maxSalasGratuito);
        plan.setMaxEquiposPorSala(maxEquiposGratuito);
        return planRepository.save(plan);
    }

    /** Crea el registro inicial en modo GRATUITO. */
    private Plan crearPlanGratis() {
        Plan plan = new Plan();
        plan.setNombre(Plan.GRATUITO);
        plan.setPrecio(0L);
        plan.setMaxSalas(maxSalasGratuito);
        plan.setMaxEquiposPorSala(maxEquiposGratuito);
        plan.setEstadoPago(Plan.PAGO_PENDIENTE);
        plan.setReferenciaPago(null);
        plan.setFechaActivacion(null);
        return planRepository.save(plan);
    }

    /** Estado del plan para el frontend. */
    public PlanResponse obtenerEstado() {
        Plan plan = obtenerPlanActivo();
        int salasRegistradas = (int) salaRepository.count();
        int equiposRegistrados = (int) equipoRepository.count();

        return new PlanResponse(
                plan.getNombre(),
                plan.getPrecio(),
                plan.getMaxSalas(),
                plan.getMaxEquiposPorSala(),
                plan.getMaxSalas() == null,
                plan.getMaxEquiposPorSala() == null,
                plan.getEstadoPago(),
                plan.getReferenciaPago(),
                plan.getFechaActivacion(),
                salasRegistradas,
                equiposRegistrados
        );
    }

    /**
     * Activa el plan Premium con compra simulada.
     *
     * Si ya está pagado, no hace nada y devuelve el plan actual.
     */
    @Transactional
    public Plan activarPremium() {
        Plan plan = obtenerPlanActivo();

        if (Plan.PAGO_PAGADO.equals(plan.getEstadoPago()) && Plan.PREMIUM.equals(plan.getNombre())) {
            return plan;
        }

        plan.setNombre(Plan.PREMIUM);
        plan.setPrecio(precioPremium);
        plan.setMaxSalas(null);
        plan.setMaxEquiposPorSala(null);
        plan.setEstadoPago(Plan.PAGO_PAGADO);
        plan.setReferenciaPago("SIM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        plan.setFechaActivacion(LocalDateTime.now());
        return planRepository.save(plan);
    }

    /**
     * Cancela el Premium y vuelve al plan gratuito.
     *
     * Cancelar no borra nada: el inventario que ya se registró se queda como
     * está, solo vuelven los topes. Por eso no se bloquea aunque ya haya más
     * aulas de las que el gratuito permite: el sistema sigue mostrando todo y
     * simplemente no deja registrar más hasta volver a contratar.
     *
     * Si ya está en gratuito, no hace nada y devuelve el plan actual, para que
     * pulsar dos veces el botón no rompa nada.
     */
    @Transactional
    public Plan cancelarPremium() {
        Plan plan = obtenerPlanActivo();

        if (Plan.GRATUITO.equals(plan.getNombre())) {
            return plan;
        }

        plan.setNombre(Plan.GRATUITO);
        plan.setPrecio(0L);
        plan.setMaxSalas(maxSalasGratuito);
        plan.setMaxEquiposPorSala(maxEquiposGratuito);
        plan.setEstadoPago(Plan.PAGO_CANCELADO);
        plan.setReferenciaPago(null);
        plan.setFechaActivacion(null);
        return planRepository.save(plan);
    }

    /**
     * Precio del Premium tal y como se muestra en pantalla. n
     *
     * Se formatea con la configuración regional de Colombia y no con la del
     * servidor, para que el mensaje del límite salga siempre como "$150.000" y
     * no como "150,000" si la máquina que corre el backend está en otro país.
     */
    private String precioPremiumFormato() {
        return "$" + new DecimalFormat(
                        "#,##0",
                        new DecimalFormatSymbols(Locale.forLanguageTag("es-CO")))
                .format(precioPremium);
    }

    /** Lanza excepción si se quiere crear otra sala y ya se alcanzó el límite. */
    public void verificarLimiteSalas() {
        Plan plan = obtenerPlanActivo();
        int salasRegistradas = (int) salaRepository.count();

        if (plan.getMaxSalas() != null && salasRegistradas >= plan.getMaxSalas()) {
            throw new RuntimeException(
                    "Límite alcanzado: el plan " + plan.getNombre() + " permite "
                            + plan.getMaxSalas() + " sala" + (plan.getMaxSalas() == 1 ? "" : "s")
                            + " y ya registraste " + salasRegistradas
                            + ". Contrata el plan Premium por " + precioPremiumFormato()
                            + " para registrar las aulas que necesite la institución."
            );
        }
    }

    /** Lanza excepción si se quiere crear otro equipo en la sala y se superó el límite. */
    public void verificarLimiteEquiposPorSala(Long idSala) {
        Plan plan = obtenerPlanActivo();
        int equiposEnSala = (int) equipoRepository.countBySalaId(idSala);

        if (plan.getMaxEquiposPorSala() != null && equiposEnSala >= plan.getMaxEquiposPorSala()) {
            throw new RuntimeException(
                    "Límite alcanzado: el plan " + plan.getNombre() + " permite "
                            + plan.getMaxEquiposPorSala() + " equipos por sala y esta ya tiene "
                            + equiposEnSala + ". Contrata el plan Premium por "
                            + precioPremiumFormato() + " para registrar los equipos que falten."
            );
        }
    }
}