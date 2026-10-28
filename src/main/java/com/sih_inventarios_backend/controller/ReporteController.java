package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes/inventario")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> inventarioPdf() {
        try {
            byte[] pdf = reporteService.generarInventarioPdf();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "inventario_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> inventarioExcel() {
        try {
            byte[] excel = reporteService.generarInventarioExcel();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "inventario_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok().headers(headers).body(excel);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/historial/{idEquipo}/pdf")
    public ResponseEntity<byte[]> historialEquipoPdf(@PathVariable Long idEquipo) {
        try {
            byte[] pdf = reporteService.generarHistorialEquipoPdf(idEquipo);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "historial_equipo_" + idEquipo + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/historial/{idEquipo}/excel")
    public ResponseEntity<byte[]> historialEquipoExcel(@PathVariable Long idEquipo) {
        try {
            byte[] excel = reporteService.generarHistorialEquipoExcel(idEquipo);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "historial_equipo_" + idEquipo + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok().headers(headers).body(excel);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/historial/equipo/{idEquipo}/pdf")
    public ResponseEntity<byte[]> historialEquipoPdfAlt(@PathVariable Long idEquipo) {
        return historialEquipoPdf(idEquipo);
    }

    @GetMapping("/historial/equipo/{idEquipo}/excel")
    public ResponseEntity<byte[]> historialEquipoExcelAlt(@PathVariable Long idEquipo) {
        return historialEquipoExcel(idEquipo);
    }

    @GetMapping("/equipo/{idEquipo}/historial/pdf")
    public ResponseEntity<byte[]> historialEquipoPdfAlt2(@PathVariable Long idEquipo) {
        return historialEquipoPdf(idEquipo);
    }

    @GetMapping("/equipo/{idEquipo}/historial/excel")
    public ResponseEntity<byte[]> historialEquipoExcelAlt2(@PathVariable Long idEquipo) {
        return historialEquipoExcel(idEquipo);
    }
}
