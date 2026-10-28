package com.sih_inventarios_backend.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
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

    public ReporteService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
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
}
