package com.sih_inventarios_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security para una API stateless con JWT.
 *
 * No hay sesiones ni formularios: el estado viaja en el token que el
 * cliente manda en la cabecera Authorization.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El navegador manda un OPTIONS antes de cada petición
                        // con cabecera Authorization: debe pasar siempre.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints públicos: el usuario aún no tiene token aquí.
                        .requestMatchers(
                                "/api/usuarios/login",
                                "/api/usuarios/registro",
                                "/api/usuarios/roles"
                        ).permitAll()
                        .requestMatchers("/error").permitAll()
                        // El plan se puede leer desde cualquier panel (los tres
                        // roles necesitan ver los límites).
                        .requestMatchers(HttpMethod.GET, "/api/plan").authenticated()
                        // Comprar y cancelar el Premium lo puede cualquiera con
                        // sesión: el plan es de la institución, no de un usuario,
                        // así que basta con estar dentro del sistema. El cobro
                        // sigue siendo simulado, no hay pasarela detrás.
                        .requestMatchers(HttpMethod.POST, "/api/plan/comprar", "/api/plan/cancelar").authenticated()
                        // El perfil propio lo necesita cualquiera con sesión.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        // La gestión de cuentas es exclusiva del administrador.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/usuarios/*", "/api/usuarios/*/*").hasRole("ADMINISTRADOR")
                        // Alta del inventario: los tres roles. El docente
                        // registra aulas y equipos porque es quien está en el
                        // sitio y llega primero.
                        .requestMatchers(HttpMethod.POST, "/api/salas", "/api/salas/*").hasAnyRole("ADMINISTRADOR", "TECNICO", "DOCENTE")
                        .requestMatchers(HttpMethod.POST, "/api/equipos", "/api/equipos/*").hasAnyRole("ADMINISTRADOR", "TECNICO", "DOCENTE")
                        // Corregir un registro ya existente es otra cosa: el
                        // docente da de alta lo que ve, pero arreglarlo después
                        // (cambiar un código, una capacidad, trasladar un equipo
                        // de aula) es del técnico y de la administración, que son
                        // quienes tienen el inventario conciliado.
                        .requestMatchers(HttpMethod.PUT, "/api/salas/*").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        .requestMatchers(HttpMethod.PUT, "/api/equipos/*").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        // Cambiar el estado, atender una falla, verificar un aula
                        // y registrar un mantenimiento quedan reservados al
                        // técnico y a la administración. Reportar una falla sí es
                        // del docente: es su función en el aula.
                        .requestMatchers(HttpMethod.PATCH, "/api/equipos/*/estado").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/fallas/*/estado").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        .requestMatchers(HttpMethod.POST, "/api/mantenimientos", "/api/mantenimientos/*").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        .requestMatchers(HttpMethod.POST, "/api/verificaciones").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        // El resto (listados, historial, resumen, conteos) es de
                        // consulta y lo puede hacer cualquier usuario con sesión.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, excepcion) -> {
                            // Respuesta JSON legible: el frontend muestra este texto
                            // en lugar del 401 vacío que devuelve Spring por defecto.
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"message\":\"No autorizado: inicia sesión para continuar.\"}");
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Se usan comodines porque el frontend corre en otro puerto. Al no
        // mandar cookies (el token va en la cabecera), no hace falta allowCredentials.
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
