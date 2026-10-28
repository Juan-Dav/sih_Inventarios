package com.sih_inventarios_backend.config;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.entity.Mantenimiento;
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rellena el aula con doce equipos de ejemplo y su historial.
 *
 * El sistema se probaba con una sola máquina y las pantallas de inventario,
 * historial y reportes quedaban casi vacías. Este cargador deja un aula
 * completa, con equipos operativos, en mantenimiento y fuera de servicio, y con
 * fallas abiertas, en revisión y resueltas (estas últimas con su mantenimiento)
 * para que los reportes del aula tengan contenido real.
 *
 * Es idempotente: solo crea lo que falta hasta llegar a doce equipos en el
 * aula, y si ya están todos no toca nada. Se apaga con la propiedad
 * `app.demo.seed=false` cuando ya no se quieran datos de demostración.
 */
@Component
@ConditionalOnProperty(
        name = "app.demo.seed",
        havingValue = "true",
        matchIfMissing = true)
public class DatosDemostracion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemostracion.class);

    /** El aula de ejemplo siempre queda con doce equipos, que es su capacidad. */
    private static final int EQUIPOS_POR_AULA = 12;

    private final SalaRepository salaRepository;
    private final EquipoRepository equipoRepository;
    private final FallaRepository fallaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    public DatosDemostracion(
            SalaRepository salaRepository,
            EquipoRepository equipoRepository,
            FallaRepository fallaRepository,
            MantenimientoRepository mantenimientoRepository) {
        this.salaRepository = salaRepository;
        this.equipoRepository = equipoRepository;
        this.fallaRepository = fallaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Sala sala = obtenerOCrearAula();
        List<Equipo> existentes = equipoRepository.findBySalaId(sala.getId());
        int faltan = EQUIPOS_POR_AULA - existentes.size();

        if (faltan <= 0) {
            log.info("Datos de demostración: el aula '{}' ya tiene {} equipos.", sala.getNombre(), existentes.size());
            return;
        }

        List<Equipo> creados = crearEquipos(sala, existentes, faltan);
        crearHistorial(creados);
        log.info("Datos de demostración: se agregaron {} equipos al aula '{}'.", creados.size(), sala.getNombre());
    }

    private Sala obtenerOCrearAula() {
        return salaRepository.findAll().stream()
                .min((una, otra) -> Long.compare(una.getId(), otra.getId()))
                .map(sala -> {
                    if (sala.getCapacidadMaxima() < EQUIPOS_POR_AULA) {
                        sala.setCapacidadMaxima(EQUIPOS_POR_AULA);
                        return salaRepository.save(sala);
                    }
                    return sala;
                })
                .orElseGet(() -> {
                    Sala nueva = new Sala();
                    nueva.setNombre("Aula de Sistemas 1");
                    nueva.setCapacidadMaxima(EQUIPOS_POR_AULA);
                    return salaRepository.save(nueva);
                });
    }

    private List<Equipo> crearEquipos(Sala sala, List<Equipo> existentes, int faltan) {
        // Los códigos siguen la serie PC-00N que ya usa el inventario; se buscan
        // los primeros números libres para no chocar con equipos del usuario.
        Set<String> ocupados = new HashSet<>();
        existentes.forEach(equipo -> ocupados.add(equipo.getCodigo()));
        equipoRepository.findAll().forEach(equipo -> ocupados.add(equipo.getCodigo()));

        // Se toman las últimas fichas de la plantilla para que un aula que ya
        // tenía equipos reciba también los estados "fuera de servicio".
        int desde = Math.max(0, PLANTILLA.length - faltan);
        List<Equipo> creados = new ArrayList<>();
        int numero = 1;

        for (int i = desde; i < PLANTILLA.length; i++) {
            String codigo;
            do {
                codigo = String.format("PC-%03d", numero++);
            } while (ocupados.contains(codigo));
            ocupados.add(codigo);

            Ficha ficha = PLANTILLA[i];
            Equipo equipo = new Equipo();
            equipo.setCodigo(codigo);
            equipo.setNombreEquipo(ficha.nombre());
            equipo.setCategoria(ficha.categoria());
            equipo.setCaracteristicas(ficha.caracteristicas());
            equipo.setMarca(ficha.marca());
            equipo.setModelo(ficha.modelo());
            equipo.setNumeroSerie("SN-" + ficha.marca().substring(0, 2).toUpperCase() + "-" + (1000 + i));
            equipo.setFechaAdquisicion(LocalDate.now().minusMonths(14 + i));
            equipo.setGarantiaHasta(LocalDate.now().plusMonths(10 - (i % 6)));
            equipo.setUbicacionDetalle("Puesto " + (i + 1));
            equipo.setObservaciones("Equipo de demostración del inventario.");
            equipo.setActivo(true);
            equipo.setEstado(ficha.estado());
            equipo.setSala(sala);

            creados.add(equipoRepository.save(equipo));
        }

        return creados;
    }

    /**
     * Historial de los equipos recién creados.
     *
     * Las fallas resueltas llevan su mantenimiento asociado (que es lo que hace
     * el flujo real al atender una falla) y las abiertas quedan pendientes para
     * que el técnico tenga trabajo en la cola.
     */
    private void crearHistorial(List<Equipo> equipos) {
        if (equipos.isEmpty()) {
            return;
        }

        crearFalla(equipo(equipos, 6), "F-DEMO-01", "El equipo no enciende: la fuente de poder no da voltaje.",
                "PENDIENTE", "ALTA", 6, null);
        crearFalla(equipo(equipos, 7), "F-DEMO-02", "La pantalla muestra líneas verticales y se apaga sola.",
                "PENDIENTE", "CRITICA", 3, null);
        crearFalla(equipo(equipos, 9), "F-DEMO-03", "El teclado no responde y el puerto USB está flojo.",
                "PENDIENTE", "MEDIA", 2, null);
        crearFalla(equipo(equipos, 8), "F-DEMO-04", "El ventilador hace ruido y el equipo se calienta.",
                "EN_REVISION", "MEDIA", 9, null);
        crearFalla(equipo(equipos, 10), "F-DEMO-05", "No arranca el sistema operativo: disco con sectores dañados.",
                "EN_REVISION", "ALTA", 5, null);

        Falla resuelta1 = crearFalla(equipo(equipos, 0), "F-DEMO-06",
                "El equipo se reiniciaba solo durante las clases.", "RESUELTO", "MEDIA", 21, 19);
        Falla resuelta2 = crearFalla(equipo(equipos, 1), "F-DEMO-07",
                "No reconocía la memoria RAM instalada.", "RESUELTO", "ALTA", 15, 14);
        Falla resuelta3 = crearFalla(equipo(equipos, 2), "F-DEMO-08",
                "El puerto de red no daba conexión a internet.", "RESUELTO", "BAJA", 30, 28);

        crearMantenimiento(equipo(equipos, 0), resuelta1, "Fuente de poder inestable por variaciones de voltaje.",
                "Se reemplazó la fuente por una de 500 W y se instaló una regleta reguladora.", 19);
        crearMantenimiento(equipo(equipos, 1), resuelta2, "Memoria RAM mal instalada después de una limpieza.",
                "Se retiró la memoria, se limpiaron los contactos y se volvió a encajar.", 14);
        crearMantenimiento(equipo(equipos, 2), resuelta3, "Cable de red en mal estado.",
                "Se reemplazó el cable UTP y se probó la conexión con el switch del aula.", 28);
        crearMantenimiento(equipo(equipos, 3), null, "Mantenimiento preventivo de mitad de semestre.",
                "Limpieza interna, cambio de pasta térmica y revisión de los ventiladores.", 4);
    }

    private Falla crearFalla(Equipo equipo, String codigo, String descripcion, String estado,
                             String prioridad, int diasAtras, Integer diasResolucion) {
        if (equipo == null) {
            return null;
        }

        Falla falla = new Falla();
        falla.setCodigoReporte(codigo);
        falla.setDescripcion(descripcion);
        falla.setEstado(estado);
        falla.setPrioridad(prioridad);
        falla.setFechaReporte(LocalDateTime.now().minusDays(diasAtras).minusHours(2));
        falla.setEquipo(equipo);

        if ("EN_REVISION".equals(estado)) {
            falla.setFechaAsignacion(LocalDateTime.now().minusDays(Math.max(1, diasAtras - 1)));
        }

        if (diasResolucion != null) {
            falla.setFechaAsignacion(LocalDateTime.now().minusDays(diasAtras - 1));
            falla.setFechaResolucion(LocalDateTime.now().minusDays(diasResolucion));
            falla.setAccionesRealizadas("Atendido por el técnico del aula; el equipo quedó operativo.");
        }

        return fallaRepository.save(falla);
    }

    private void crearMantenimiento(Equipo equipo, Falla falla, String diagnostico,
                                    String solucion, int diasAtras) {
        if (equipo == null) {
            return;
        }

        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setEquipo(equipo);
        mantenimiento.setFalla(falla);
        mantenimiento.setDiagnostico(diagnostico);
        mantenimiento.setSolucion(solucion);
        mantenimiento.setFechaMantenimiento(LocalDateTime.now().minusDays(diasAtras).minusHours(1));
        mantenimientoRepository.save(mantenimiento);
    }

    /** La ficha en la posición indicada, o null si el aula tenía menos equipos nuevos. */
    private static Equipo equipo(List<Equipo> equipos, int indice) {
        return indice < equipos.size() ? equipos.get(indice) : null;
    }

    /** Modelo de cada una de las doce máquinas del aula de ejemplo. */
    private record Ficha(String nombre, String categoria, String caracteristicas,
                         String marca, String modelo, String estado) {
    }

    private static final Ficha[] PLANTILLA = {
            new Ficha("Computador de escritorio", "Computador", "Intel Core i5, 8 GB RAM, SSD 480 GB",
                    "Dell", "OptiPlex 3080", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i5, 16 GB RAM, SSD 512 GB",
                    "Lenovo", "ThinkCentre M70q", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "AMD Ryzen 5, 8 GB RAM, SSD 480 GB",
                    "HP", "ProDesk 405 G8", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i3, 8 GB RAM, HDD 1 TB",
                    "Asus", "ExpertCenter D500", "EN_MANTENIMIENTO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i7, 16 GB RAM, SSD 1 TB",
                    "Dell", "OptiPlex 7010", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i3, 4 GB RAM, HDD 500 GB",
                    "Acer", "Veriton X2660G", "FUERA_DE_SERVICIO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i5, 8 GB RAM, SSD 256 GB",
                    "HP", "EliteDesk 800 G5", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "AMD Ryzen 3, 8 GB RAM, SSD 480 GB",
                    "Lenovo", "IdeaCentre 3", "EN_MANTENIMIENTO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i5, 12 GB RAM, SSD 512 GB",
                    "Dell", "Vostro 3681", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i7, 16 GB RAM, SSD 1 TB",
                    "HP", "ProDesk 600 G6", "OPERATIVO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i3, 8 GB RAM, HDD 1 TB",
                    "Asus", "ExpertCenter D700", "EN_MANTENIMIENTO"),
            new Ficha("Computador de escritorio", "Computador", "Intel Core i5, 8 GB RAM, SSD 480 GB",
                    "Acer", "Veriton M4660G", "FUERA_DE_SERVICIO"),
    };
}
