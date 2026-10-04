package com.sih_inventarios_backend.controller;

import com.sih_inventarios_backend.dto.LoginRequest;
import com.sih_inventarios_backend.dto.LoginResponse;
import com.sih_inventarios_backend.entity.Rol;
import com.sih_inventarios_backend.entity.Usuario;
import com.sih_inventarios_backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
// El CORS lo resuelve el CorsConfigurationSource de SecurityConfig.
// Añadir @CrossOrigin aquí duplicaría la cabecera Access-Control-Allow-Origin
// y el navegador rechazaría la respuesta.
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * 1. Registra un usuario.
     *
     * Ruta pública. Quien se registra elige su rol entre ADMINISTRADOR, TECNICO y
     * DOCENTE; si no lo manda, se aplica DOCENTE. La cuenta queda ACTIVA de
     * inmediato y el administrador puede corregir el rol desde su panel.
     *
     * Ojo: permitir que el registro público cree administradores significa que
     * cualquiera que llegue al formulario puede quedarse con control del sistema.
     * Es aceptable mientras esto sea una práctica, pero en un despliegue real el
     * alta de ADMINISTRADOR debería quedar reservada al panel. Se hace quitando
     * `ROL_ADMINISTRADOR` de la lista de roles válidos de más abajo, o mejor,
     * pasando el alta por un código de invitación.
     *
     * El cuerpo no lleva @Valid en los métodos de actualización: la contraseña es
     * obligatoria solo al crear, y validarla ahí dejaría pasar cualquier edición
     * de nombre o correo.
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(
            @Valid @RequestBody Usuario usuario,
            @RequestParam(required = false) String rol) {

        String rolEfectivo;
        if (rol != null && !rol.isBlank()) {
            rolEfectivo = rol.trim().toUpperCase();
        } else {
            rolEfectivo = UsuarioService.ROL_DOCENTE;
        }

        // Validar que el rol sea válido
        if (!UsuarioService.ROL_ADMINISTRADOR.equals(rolEfectivo) &&
                !UsuarioService.ROL_TECNICO.equals(rolEfectivo) &&
                !UsuarioService.ROL_DOCENTE.equals(rolEfectivo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Rol no válido. Usa DOCENTE, TECNICO o ADMINISTRADOR."));
        }

        try {
            return ResponseEntity.ok(usuarioService.registrarUsuario(usuario, rolEfectivo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. Inicio de sesión: valida las credenciales y devuelve un token JWT
    // con el rol dentro. Es público: el usuario todavía no tiene token.
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse respuesta = usuarioService.autenticar(request.correo(), request.contrasena());
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Devuelve los datos del usuario del token. Sirve para refrescar
    // la sesión al recargar la página sin volver a pedir el token.
    @GetMapping("/me")
    public ResponseEntity<?> miPerfil(@AuthenticationPrincipal String correo) {
        return usuarioService.buscarPorCorreo(correo)
                .map(usuario -> ResponseEntity.ok(Map.of(
                        "id", usuario.getId(),
                        "nombre", usuario.getNombre(),
                        "correo", usuario.getCorreo(),
                        "rol", usuario.getRol() != null ? usuario.getRol().getNombre() : "",
                        "estado", usuario.getEstado()
                )))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("message", "El usuario del token ya no existe.")));
    }

    // 4. Listado completo de cuentas: solo el administrador del panel lo usa.
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Usuario> obtenerUsuarios() {
        return usuarioService.obtenerTodosLosUsuarios();
    }

    // 5. Modifica nombre y correo: PUT /api/usuarios/1
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario cambios) {
        try {
            return ResponseEntity.ok(usuarioService.actualizarDatos(id, cambios));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. Asigna un rol: PATCH /api/usuarios/1/rol?rol=TECNICO
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> asignarRol(@PathVariable Long id, @RequestParam String rol) {
        try {
            return ResponseEntity.ok(usuarioService.asignarRol(id, rol));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. Activa o desactiva una cuenta: PATCH /api/usuarios/1/estado?estado=INACTIVO
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        try {
            return ResponseEntity.ok(usuarioService.cambiarEstado(id, estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 8. Endpoint para listar los roles disponibles (lo llena el <select> del formulario de registro)
    @GetMapping("/roles")
    public List<Rol> obtenerRoles() {
        return usuarioService.obtenerRoles();
    }
}