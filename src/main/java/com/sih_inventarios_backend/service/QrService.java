package com.sih_inventarios_backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sih_inventarios_backend.entity.Equipo;
import com.sih_inventarios_backend.repository.EquipoRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class QrService {

    private final EquipoRepository equipoRepository;

    public QrService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public Equipo generarQr(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("El equipo no existe"));
        if (equipo.getCodigoQr() == null || equipo.getCodigoQr().isBlank()) {
            equipo.setCodigoQr(UUID.randomUUID().toString());
            equipo.setRutaQr("/api/consulta/qr/" + equipo.getCodigoQr());
            equipoRepository.save(equipo);
        }
        return equipo;
    }

    public byte[] generarImagen(String codigoQr, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(codigoQr, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}
