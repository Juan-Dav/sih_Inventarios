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
import com.sih_inventarios_backend.entity.Sala;
import com.sih_inventarios_backend.repository.EquipoRepository;
import com.sih_inventarios_backend.repository.FallaRepository;
import com.sih_inventarios_backend.repository.MantenimientoRepository;
import com.sih_inventarios_backend.repository.SalaRepository;
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
    private final SalaRepository salaRepository;

    public ReporteService(EquipoRepository equipoRepository, FallaRepository fallaRepository,
                          MantenimientoRepository mantenimientoRepository, SalaRepository salaRepository) {
        this.equipoRepository = equipoRepository;
        this.fallaRepository = fallaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.salaRepository = salaRepository;
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

    /**
     * Historial completo de un aula en un solo PDF.
     *
     * Antes, para descargar el historial de un aula el navegador tenía que pedir
     * un reporte por equipo y bajar un archivo por cada uno. Aquí se unen todos:
     * una página de portada con el resumen del aula y una página por equipo con
     * sus datos, sus fallas y sus mantenimientos.
     */
    public byte[] generarHistorialSalaPdf(Long salaId) throws DocumentException {
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RuntimeException("El aula no existe"));
        List<Equipo> equipos = equipoRepository.findBySalaId(salaId);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("HISTORIAL DEL AULA", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph nombre = new Paragraph(valor(sala.getNombre()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        nombre.setAlignment(Element.ALIGN_CENTER);
        document.add(nombre);
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Fecha de generación: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(new Paragraph("Equipos registrados: " + equipos.size()
                + " / Capacidad del aula: " + sala.getCapacidadMaxima()));
        document.add(new Paragraph("Fallas reportadas: " + contarFallas(equipos)));
        document.add(new Paragraph("Mantenimientos registrados: " + contarMantenimientos(equipos)));
        document.add(Chunk.NEWLINE);

        if (equipos.isEmpty()) {
            document.add(new Paragraph("El aula no tiene equipos registrados."));
        }

        for (Equipo equipo : equipos) {
            document.newPage();
            agregarBloqueEquipoPdf(document, equipo);
        }

        document.close();
        return out.toByteArray();
    }

    /** Una ficha de equipo dentro del reporte del aula. */
    private void agregarBloqueEquipoPdf(Document document, Equipo equipo) throws DocumentException {
        List<Falla> fallas = fallaRepository.findByEquipoIdOrderByFechaReporteDesc(equipo.getId());
        List<Mantenimiento> mantenimientos = mantenimientoRepository
                .findByEquipoIdOrderByFechaMantenimientoDesc(equipo.getId());

        document.add(new Paragraph("Equipo: " + valor(equipo.getCodigo()) + " - " + valor(equipo.getNombreEquipo()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Marca: " + valor(equipo.getMarca())));
        document.add(new Paragraph("Modelo: " + valor(equipo.getModelo())));
        document.add(new Paragraph("Serie: " + valor(equipo.getNumeroSerie())));
        document.add(new Paragraph("Estado: " + valor(equipo.getEstado())));
        document.add(new Paragraph("Características: " + valor(equipo.getCaracteristicas())));
        document.add(new Paragraph("Sala: " + (equipo.getSala() != null ? valor(equipo.getSala().getNombre()) : "")));
        document.add(new Paragraph("Área: " + (equipo.getArea() != null ? valor(equipo.getArea().getNombre()) : "")));
        document.add(new Paragraph("Departamento: "
                + (equipo.getDepartamento() != null ? valor(equipo.getDepartamento().getNombre()) : "")));
        document.add(new Paragraph("Responsable: "
                + (equipo.getResponsableCustodia() != null
                        ? valor(equipo.getResponsableCustodia().getNombreCompleto())
                        : "")));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Fallas reportadas (" + fallas.size() + ")",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (fallas.isEmpty()) {
            document.add(new Paragraph("Sin fallas registradas"));
        } else {
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] {2f, 5f, 2f, 2f});
            for (String h : new String[] {"Fecha", "Descripción", "Prioridad", "Estado"}) {
                PdfPCell celda = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }
            for (Falla falla : fallas) {
                tabla.addCell(falla.getFechaReporte() != null ? falla.getFechaReporte().toString() : "");
                tabla.addCell(valor(falla.getDescripcion()));
                tabla.addCell(valor(falla.getPrioridad()));
                tabla.addCell(valor(falla.getEstado()));
            }
            document.add(tabla);
        }
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Mantenimientos (" + mantenimientos.size() + ")",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (mantenimientos.isEmpty()) {
            document.add(new Paragraph("Sin mantenimientos registrados"));
        } else {
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] {2f, 4f, 4f});
            for (String h : new String[] {"Fecha", "Diagnóstico", "Solución"}) {
                PdfPCell celda = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }
            for (Mantenimiento mantenimiento : mantenimientos) {
                tabla.addCell(mantenimiento.getFechaMantenimiento() != null
                        ? mantenimiento.getFechaMantenimiento().toString()
                        : "");
                tabla.addCell(valor(mantenimiento.getDiagnostico()));
                tabla.addCell(valor(mantenimiento.getSolucion()));
            }
            document.add(tabla);
        }
    }

    /**
     * Historial completo de un aula en un solo Excel.
     *
     * Una hoja con el resumen del aula y, debajo, un bloque por equipo con sus
     * fallas y sus mantenimientos. Es el mismo contenido del PDF, pero en un
     * formato que luego se puede filtrar o sumar.
     */
    public byte[] generarHistorialSalaExcel(Long salaId) throws IOException {
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RuntimeException("El aula no existe"));
        List<Equipo> equipos = equipoRepository.findBySalaId(salaId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Aula");

        CellStyle titulo = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuenteTitulo = workbook.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);
        titulo.setFont(fuenteTitulo);

        CellStyle seccion = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuenteSeccion = workbook.createFont();
        fuenteSeccion.setBold(true);
        fuenteSeccion.setFontHeightInPoints((short) 11);
        seccion.setFont(fuenteSeccion);

        CellStyle encabezado = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuenteEncabezado = workbook.createFont();
        fuenteEncabezado.setBold(true);
        encabezado.setFont(fuenteEncabezado);

        int fila = 0;
        fila = escribirFila(sheet, fila, titulo, "HISTORIAL DEL AULA: " + valor(sala.getNombre()));
        fila = escribirFila(sheet, fila, null, "Fecha de generación",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        fila = escribirFila(sheet, fila, null, "Equipos registrados", String.valueOf(equipos.size()));
        fila = escribirFila(sheet, fila, null, "Capacidad del aula", String.valueOf(sala.getCapacidadMaxima()));
        fila = escribirFila(sheet, fila, null, "Fallas reportadas", String.valueOf(contarFallas(equipos)));
        fila = escribirFila(sheet, fila, null, "Mantenimientos", String.valueOf(contarMantenimientos(equipos)));
        fila++;

        for (Equipo equipo : equipos) {
            List<Falla> fallas = fallaRepository.findByEquipoIdOrderByFechaReporteDesc(equipo.getId());
            List<Mantenimiento> mantenimientos = mantenimientoRepository
                    .findByEquipoIdOrderByFechaMantenimientoDesc(equipo.getId());

            fila = escribirFila(sheet, fila, seccion,
                    "EQUIPO: " + valor(equipo.getCodigo()) + " - " + valor(equipo.getNombreEquipo()));
            fila = escribirFila(sheet, fila, null, "Marca", valor(equipo.getMarca()));
            fila = escribirFila(sheet, fila, null, "Modelo", valor(equipo.getModelo()));
            fila = escribirFila(sheet, fila, null, "Serie", valor(equipo.getNumeroSerie()));
            fila = escribirFila(sheet, fila, null, "Estado", valor(equipo.getEstado()));
            fila = escribirFila(sheet, fila, null, "Características", valor(equipo.getCaracteristicas()));
            fila = escribirFila(sheet, fila, null, "Sala",
                    equipo.getSala() != null ? valor(equipo.getSala().getNombre()) : "");
            fila = escribirFila(sheet, fila, null, "Área",
                    equipo.getArea() != null ? valor(equipo.getArea().getNombre()) : "");
            fila = escribirFila(sheet, fila, null, "Departamento",
                    equipo.getDepartamento() != null ? valor(equipo.getDepartamento().getNombre()) : "");
            fila = escribirFila(sheet, fila, null, "Responsable",
                    equipo.getResponsableCustodia() != null
                            ? valor(equipo.getResponsableCustodia().getNombreCompleto())
                            : "");

            fila = escribirFila(sheet, fila, encabezado, "FALLAS - Fecha", "Descripción", "Prioridad", "Estado");
            if (fallas.isEmpty()) {
                fila = escribirFila(sheet, fila, null, "Sin fallas registradas");
            } else {
                for (Falla falla : fallas) {
                    fila = escribirFila(sheet, fila, null,
                            falla.getFechaReporte() != null ? falla.getFechaReporte().toString() : "",
                            valor(falla.getDescripcion()),
                            valor(falla.getPrioridad()),
                            valor(falla.getEstado()));
                }
            }

            fila = escribirFila(sheet, fila, encabezado,
                    "MANTENIMIENTOS - Fecha", "Diagnóstico", "Solución");
            if (mantenimientos.isEmpty()) {
                fila = escribirFila(sheet, fila, null, "Sin mantenimientos registrados");
            } else {
                for (Mantenimiento mantenimiento : mantenimientos) {
                    fila = escribirFila(sheet, fila, null,
                            mantenimiento.getFechaMantenimiento() != null
                                    ? mantenimiento.getFechaMantenimiento().toString()
                                    : "",
                            valor(mantenimiento.getDiagnostico()),
                            valor(mantenimiento.getSolucion()));
                }
            }
            fila += 2;
        }

        sheet.setColumnWidth(0, 26 * 256);
        sheet.setColumnWidth(1, 46 * 256);
        sheet.setColumnWidth(2, 22 * 256);
        sheet.setColumnWidth(3, 22 * 256);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    /** Escribe una fila y devuelve el número de la siguiente. */
    private int escribirFila(Sheet sheet, int numero, CellStyle estilo, String... valores) {
        org.apache.poi.ss.usermodel.Row fila = sheet.createRow(numero);
        for (int columna = 0; columna < valores.length; columna++) {
            org.apache.poi.ss.usermodel.Cell celda = fila.createCell(columna);
            celda.setCellValue(valor(valores[columna]));
            if (estilo != null) {
                celda.setCellStyle(estilo);
            }
        }
        return numero + 1;
    }

    private int contarFallas(List<Equipo> equipos) {
        return equipos.stream()
                .mapToInt(equipo -> fallaRepository.findByEquipoIdOrderByFechaReporteDesc(equipo.getId()).size())
                .sum();
    }

    private int contarMantenimientos(List<Equipo> equipos) {
        return equipos.stream()
                .mapToInt(equipo -> mantenimientoRepository
                        .findByEquipoIdOrderByFechaMantenimientoDesc(equipo.getId()).size())
                .sum();
    }

    /** Convierte null en cadena vacía: los PDF y el Excel no deben imprimir "null". */
    private static String valor(Object dato) {
        return dato != null ? dato.toString() : "";
    }
}
