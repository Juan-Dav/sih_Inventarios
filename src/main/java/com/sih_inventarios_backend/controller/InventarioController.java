package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.dto.ResumenInventarioResponse;
import com.sih_inventarios_backend.service.InventarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura consolidada del inventario.
 *
 * Sin esto el administrador tendría que cruzarse el listado de aulas, el de
 * equipos, el de fallas y el de mantenimientos para contar y comparar a mano.
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /**
     * Fotografía completa: totales, conteo por sala, equipos fuera de servicio y
     * fallas abiertas.
     */
    @GetMapping("/resumen")
    public ResumenInventarioResponse obtenerResumen() {
        return inventarioService.obtenerResumen();
    }
}
