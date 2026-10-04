package com.sih_inventarios_backend.dto;

/**
 * Respuesta del login. El frontend guarda el token y usa el rol
 * para decidir a qué dashboard redirigir.
 */
public record LoginResponse(
        String token,
        Long id,
        String nombre,
        String correo,
        String rol,
        String estado
) {
}
