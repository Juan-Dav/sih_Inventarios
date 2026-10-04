package com.sih_inventarios_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Intercepta cada petición para leer la cabecera
 * "Authorization: Bearer <token>" y, si el token es válido, deja al
 * usuario autenticado en el contexto de seguridad de Spring.
 *
 * Si el token falta o no es válido, la petición sigue como anónima:
 * es el SecurityConfig quien decide si eso es un error (401) o no.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            autenticar(cabecera.substring(7).trim(), request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            String correo = jwtService.extraerCorreo(token);
            String rol = jwtService.extraerRol(token);

            if (correo == null || rol == null) {
                return;
            }

            // Spring espera las autoridades con el prefijo "ROLE_".
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    correo,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol))
            );
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (Exception e) {
            // Token caducado o manipulado: se continúa como petición anónima.
        }
    }
}
