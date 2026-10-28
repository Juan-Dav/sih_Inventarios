package com.sih_inventarios_backend.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.entity.Falla;
import com.sih_inventarios_backend.entity.Mantenimiento;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    private final EquipoRepository equipoRepository;
    private final FallaRepository fallaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    public ReporteService(EquipoRepository equipoRepository, FallaRepository fallaRepository, MantenimientoRepository mantenimientoRepository) {
        this.equipoRepository = equipoRepository;
        this.fallaRepository = fallaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    public byte[] generarInventarioPdf() throws DocumentException {
        List<Equipo> equipos = equipoRepository.findAll();
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("INVENTARIO SIH - REPORTE PDF", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        String[] headers = {"Código", "Nombre", "Marca", "Modelo", "Serie", "Estado", "Sala", "Área", "Depto", "Responsable"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (Equipo e : equipos) {
            table.addCell(e.getCodigo() != null ? e.getCodigo() : "");
            table.addCell(e.getNombreEquipo() != null ? e.getNombreEquipo() : "");
            table.addCell(e.getMarca() != null ? e.getMarca() : "");
            table.addCell(e.getModelo() != null ? e.getModelo() : "");
            table.addCell(e.getNumeroSerie() != null ? e.getNumeroSerie() : "");
            table.addCell(e.getEstado() != null ? e.getEstado() : "");
            table.addCell(e.getSala() != null ? e.getSala().getNombre() : "");
            table.addCell(e.getArea() != null ? e.getArea().getNombre() : "");
            table.addCell(e.getDepartamento() != null ? e.getDepartamento().getNombre() : "");
            table.addCell(e.getResponsableCustodia() != null ? e.getResponsableCustodia().getNombreCompleto() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public byte[] generarInventarioExcel() throws IOException {
        List<Equipo> equipos = equipoRepository.findAll();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventario");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Código", "Nombre", "Categoría", "Marca", "Modelo", "N° Serie", "Estado", "Sala", "Área", "Departamento", "Responsable", "Fecha Adquisición", "Garantía"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Equipo e : equipos) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(e.getCodigo() != null ? e.getCodigo() : "");
            row.createCell(1).setCellValue(e.getNombreEquipo() != null ? e.getNombreEquipo() : "");
            row.createCell(2).setCellValue(e.getCategoria() != null ? e.getCategoria() : "");
            row.createCell(3).setCellValue(e.getMarca() != null ? e.getMarca() : "");
            row.createCell(4).setCellValue(e.getModelo() != null ? e.getModelo() : "");
            row.createCell(5).setCellValue(e.getNumeroSerie() != null ? e.getNumeroSerie() : "");
            row.createCell(6).setCellValue(e.getEstado() != null ? e.getEstado() : "");
            row.createCell(7).setCellValue(e.getSala() != null ? e.getSala().getNombre() : "");
            row.createCell(8).setCellValue(e.getArea() != null ? e.getArea().getNombre() : "");
            row.createCell(9).setCellValue(e.getDepartamento() != null ? e.getDepartamento().getNombre() : "");
            row.createCell(10).setCellValue(e.getResponsableCustodia() != null ? e.getResponsableCustodia().getNombreCompleto() : "");
            row.createCell(11).setCellValue(e.getFechaAdquisicion() != null ? e.getFechaAdquisicion().toString() : "");
            row.createCell(12).setCellValue(e.getGarantiaHasta() != null ? e.getGarantiaHasta().toString() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    public byte[] generarHistorialEquipoPdf(Long equipoId) throws DocumentException {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("El equipo no existe"));
        List<Falla> fallas = fallaRepository.findByEquipoIdOrderByFechaReporteDesc(equipoId);
        List<Mantenimiento> mantenimientos = mantenimientoRepository.findByEquipoIdOrderByFechaMantenimientoDesc(equipoId);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("HISTORIAL DEL EQUIPO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Datos del equipo", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        document.add(new Paragraph("Código: " + (equipo.getCodigo() != null ? equipo.getCodigo() : "")));
        document.add(new Paragraph("Nombre: " + (equipo.getNombreEquipo() != null ? equipo.getNombreEquipo() : "")));
        document.add(new Paragraph("Marca/Modelo: " + ((equipo.getMarca() != null ? equipo.getMarca() : "") + " " + (equipo.getModelo() != null ? equipo.getModelo() : "")).trim()));
        document.add(new Paragraph("Serie: " + (equipo.getNumeroSerie() != null ? equipo.getNumeroSerie() : "")));
        document.add(new Paragraph("Estado: " + (equipo.getEstado() != null ? equipo.getEstado() : "")));
        document.add(new Paragraph("Sala: " + (equipo.getSala() != null ? equipo.getSala().getNombre() : "")));
        document.add(new Paragraph("Área: " + (equipo.getArea() != null ? equipo.getArea().getNombre() : "")));
        document.add(new Paragraph("Departamento: " + (equipo.getDepartamento() != null ? equipo.getDepartamento().getNombre() : "")));
        document.add(new Paragraph("Responsable: " + (equipo.getResponsableCustodia() != null ? equipo.getResponsableCustodia().getNombreCompleto() : "")));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Fallas reportadas", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (fallas.isEmpty()) {
            document.add(new Paragraph("Sin fallas registradas"));
        } else {
            PdfPTable tableFallas = new PdfPTable(3);
            tableFallas.setWidthPercentage(100);
            String[] hF = {"Fecha", "Descripción", "Estado"};
            for (String h : hF) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tableFallas.addCell(cell);
            }
            for (Falla f : fallas) {
                tableFallas.addCell(f.getFechaReporte() != null ? f.getFechaReporte().toString() : "");
                tableFallas.addCell(f.getDescripcion() != null ? f.getDescripcion() : "");
                tableFallas.addCell(f.getEstado() != null ? f.getEstado() : "");
            }
            document.add(tableFallas);
        }
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Mantenimientos", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (mantenimientos.isEmpty()) {
            document.add(new Paragraph("Sin mantenimientos registrados"));
        } else {
            PdfPTable tableMant = new PdfPTable(3);
            tableMant.setWidthPercentage(100);
            String[] hM = {"Fecha", "Diagnóstico", "Solución"};
            for (String h : hM) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tableMant.addCell(cell);
            }
            for (Mantenimiento m : mantenimientos) {
                tableMant.addCell(m.getFechaMantenimiento() != null ? m.getFechaMantenimiento().toString() : "");
                tableMant.addCell(m.getDiagnostico() != null ? m.getDiagnostico() : "");
                tableMant.addCell(m.getSolucion() != null ? m.getSolucion() : "");
            }
            document.add(tableMant);
        }

        document.close();
        return out.toByteArray();
    }

    public byte[] generarHistorialEquipoExcel(Long equipoId) throws IOException {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("El equipo no existe"));
        List<Falla> fallas = fallaRepository.findByEquipoIdOrderByFechaReporteDesc(equipoId);
        List<Mantenimiento> mantenimientos = mantenimientoRepository.findByEquipoIdOrderByFechaMantenimientoDesc(equipoId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Historial");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        int rowNum = 0;
        org.apache.poi.ss.usermodel.Row infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Código");
        infoRow.createCell(1).setCellValue(equipo.getCodigo() != null ? equipo.getCodigo() : "");
        rowNum++;
        infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Nombre");
        infoRow.createCell(1).setCellValue(equipo.getNombreEquipo() != null ? equipo.getNombreEquipo() : "");
        rowNum++;
        infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Sala");
        infoRow.createCell(1).setCellValue(equipo.getSala() != null ? equipo.getSala().getNombre() : "");
        rowNum++;
        infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Fecha generación");
        infoRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rowNum++;

        org.apache.poi.ss.usermodel.Row headerF = sheet.createRow(rowNum++);
        String[] hF = {"FALLAS - Fecha", "Descripción", "Estado"};
        for (int i = 0; i < hF.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerF.createCell(i);
            cell.setCellValue(hF[i]);
            cell.setCellStyle(headerStyle);
        }
        for (Falla f : fallas) {
            org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue(f.getFechaReporte() != null ? f.getFechaReporte().toString() : "");
            r.createCell(1).setCellValue(f.getDescripcion() != null ? f.getDescripcion() : "");
            r.createCell(2).setCellValue(f.getEstado() != null ? f.getEstado() : "");
        }
        rowNum++;

        org.apache.poi.ss.usermodel.Row headerM = sheet.createRow(rowNum++);
        String[] hM = {"MANTENIMIENTOS - Fecha", "Diagnóstico", "Solución"};
        for (int i = 0; i < hM.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerM.createCell(i);
            cell.setCellValue(hM[i]);
            cell.setCellStyle(headerStyle);
        }
        for (Mantenimiento m : mantenimientos) {
            org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue(m.getFechaMantenimiento() != null ? m.getFechaMantenimiento().toString() : "");
            r.createCell(1).setCellValue(m.getDiagnostico() != null ? m.getDiagnostico() : "");
            r.createCell(2).setCellValue(m.getSolucion() != null ? m.getSolucion() : "");
        }

        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }
}
