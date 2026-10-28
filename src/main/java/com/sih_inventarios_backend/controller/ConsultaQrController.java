package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/consulta/qr")
public class ConsultaQrController {

    private final EquipoRepository equipoRepository;

    public ConsultaQrController(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @GetMapping("/{codigoQr}")
    public ResponseEntity<?> consultar(@PathVariable String codigoQr) {
        Optional<Equipo> equipoOpt = equipoRepository.findAll().stream()
                .filter(e -> codigoQr.equals(e.getCodigoQr()))
                .findFirst();
        if (equipoOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Código QR no válido o equipo no encontrado"));
        }
        Equipo equipo = equipoOpt.get();
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigo", equipo.getCodigo());
        datos.put("nombreEquipo", equipo.getNombreEquipo());
        datos.put("marca", equipo.getMarca());
        datos.put("modelo", equipo.getModelo());
        datos.put("estado", equipo.getEstado());
        datos.put("activo", equipo.getActivo());
        if (equipo.getSala() != null) {
            datos.put("sala", equipo.getSala().getNombre());
        }
        if (equipo.getArea() != null) {
            datos.put("area", equipo.getArea().getNombre());
        }
        if (equipo.getDepartamento() != null) {
            datos.put("departamento", equipo.getDepartamento().getNombre());
        }
        if (equipo.getResponsableCustodia() != null) {
            datos.put("responsableCustodia", equipo.getResponsableCustodia().getNombreCompleto());
            datos.put("cargoResponsable", equipo.getResponsableCustodia().getCargo());
        }
        return ResponseEntity.ok(datos);
    }
}
