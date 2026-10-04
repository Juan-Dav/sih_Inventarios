package com.sih_inventarios_backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo que envía el formulario de login. */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String contrasena
) {
}
