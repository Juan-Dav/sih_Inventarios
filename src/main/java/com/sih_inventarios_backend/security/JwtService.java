package com.sih_inventarios_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los tokens JWT.
 *
 * El payload lleva el correo como "subject" y el nombre del rol en el
 * claim "rol". El frontend no necesita firmarlos: solo los lee.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Long idUsuario, String correo, String nombreRol) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(correo)
                .claim("id", idUsuario)
                .claim("rol", nombreRol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    public String extraerCorreo(String token) {
        return leerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return leerClaims(token).get("rol", String.class);
    }

    /** @return true si el token es válido y pertenece a ese correo. */
    public boolean esValido(String token, String correo) {
        try {
            return correo != null && correo.equals(extraerCorreo(token));
        } catch (Exception e) {
            // Token mal formado, con firma inválida o caducado.
            return false;
        }
    }

    private Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
