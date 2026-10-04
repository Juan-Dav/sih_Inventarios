package com.sih_inventarios_backend.config;

import com.sih_inventarios_backend.entity.Rol;
import com.sih_inventarios_backend.entity.Usuario;
import com.sih_inventarios_backend.repository.RolRepository;
import com.sih_inventarios_backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja siempre una cuenta de administrador con la que se puede entrar.
 *
 * Nació porque el registro público solo creaba cuentas de DOCENTE y el registro
 * con rol elegido exigía un token de administrador: las dos puertas cerrados
 * dejaban al sistema sin entrada. Hoy el registro público sí deja elegir entre
 * ADMINISTRADOR, TECNICO y DOCENTE, así que la primera cuenta de administración
 * se crea desde el propio formulario.
 *
 * Por eso viene apagada en `application.properties`
 * (`app.bootstrap.admin.activado=false`): es una red de seguridad, no el flujo
 * normal, y no debe crear cuentas por su cuenta. Si algún día se cierra el
 * registro público y vuelve a hacer falta una puerta de entrada, se enciende
 * otra vez y solo actúa si no existe ningún ADMINISTRADOR.
 *
 * Mientras esté encendida, se puede apagar del todo con la misma propiedad, y
 * las credenciales se cambian en `application.properties`.
 */
@Component
@ConditionalOnProperty(
        name = "app.bootstrap.admin.activado",
        havingValue = "true",
        matchIfMissing = true)
public class BootstrapAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministrador.class);

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public BootstrapAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findFirstByRolNombre(ROL_ADMINISTRADOR).isPresent()) {
            return;
        }

        Rol rolAdmin = rolRepository.findByNombre(ROL_ADMINISTRADOR);
        if (rolAdmin == null) {
            // Los roles los inserta data.sql, que se ejecuta justo antes de esto.
            // Si aun así no están, es que la carga inicial falló y no tiene sentido
            // crear una cuenta con un rol que no existe.
            log.warn("No se creó la cuenta de administrador inicial: el rol {} no existe.", ROL_ADMINISTRADOR);
            return;
        }

        String correo = environment.getProperty("app.bootstrap.admin.correo", "admin@sih.local");
        String contrasena = environment.getProperty("app.bootstrap.admin.contrasena", "admin123");

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador del sistema");
        admin.setCorreo(correo);
        admin.setContrasena(passwordEncoder.encode(contrasena));
        admin.setEstado("ACTIVO");
        admin.setRol(rolAdmin);
        usuarioRepository.save(admin);

        log.warn("""

                ============================================================
                  Cuenta de administrador creada para que puedas entrar:
                    correo:     {}
                    contraseña: {}
                  Cámbiala desde el panel en cuanto entres.
                ============================================================
                """, correo, contrasena);
    }
}
