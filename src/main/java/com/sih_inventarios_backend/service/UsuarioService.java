package com.sih_inventarios_backend.service;

import com.sih_inventarios_backend.dto.LoginResponse;
import com.sih_inventarios_backend.entity.Rol;
import com.sih_inventarios_backend.entity.Usuario;
import com.sih_inventarios_backend.repository.RolRepository;
import com.sih_inventarios_backend.repository.UsuarioRepository;
import com.sih_inventarios_backend.security.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    /** Estados posibles de una cuenta. */
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_INACTIVO = "INACTIVO";

    /** Roles del sistema */
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_TECNICO = "TECNICO";
    public static final String ROL_DOCENTE = "DOCENTE";

    private final UsuarioRepository usuarioRepository;

    private final RolRepository rolRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    @PersistenceContext
    private EntityManager entityManager;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // 1. Método para registrar un usuario nuevo
    @Transactional
    public Usuario registrarUsuario(Usuario usuario, String nombreRol) {
        // Validamos que no se repita el correo (si no, la BD lo rechaza y saltaría un 500)
        if (usuario.getCorreo() != null && usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new RuntimeException("Error: El correo ya está registrado.");
        }

        // Buscamos el rol en la base de datos
        String rolNormalizado = nombreRol != null ? nombreRol.trim().toUpperCase() : ROL_DOCENTE;
        Rol rol = rolRepository.findByNombre(rolNormalizado);
        if (rol == null) {
            throw new RuntimeException("Error: El rol especificado no existe.");
        }

        // Le asignamos el rol al usuario
        usuario.setRol(rol);

        // La contraseña nunca se guarda en claro
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));

        // La cuenta queda usable de inmediato. Si más adelante se quiere
        // aprobación manual, aquí se dejaría en PENDIENTE.
        usuario.setEstado(ESTADO_ACTIVO);

        return usuarioRepository.save(usuario);
    }

    // 2. Método para que el Administrador vea a todos los usuarios
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    // 3. Método para buscar un usuario por su correo
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    /**
     * 4. Modifica los datos de un usuario: nombre y correo.
     *
     * La contraseña no se toca aquí a propósito: si se cambiara, dejaría de ser
     * un BCrypt y la cuenta quedaría con la contraseña en claro.
     */
    @Transactional
    public Usuario actualizarDatos(Long id, Usuario cambios) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El usuario no existe."));

        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
            usuario.setNombre(cambios.getNombre().trim());
        }

        if (cambios.getCorreo() != null && !cambios.getCorreo().isBlank()) {
            String correo = cambios.getCorreo().trim();

            Optional<Usuario> otro = usuarioRepository.findByCorreo(correo);
            if (otro.isPresent() && !otro.get().getId().equals(id)) {
                throw new RuntimeException("Error: Ese correo ya pertenece a otra cuenta.");
            }

            usuario.setCorreo(correo);
        }

        return usuarioRepository.save(usuario);
    }

    /** 5. Cambia el rol de un usuario. */
    @Transactional
    public Usuario asignarRol(Long id, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El usuario no existe."));

        Rol rol = rolRepository.findByNombre(nombreRol);
        if (rol == null) {
            throw new RuntimeException("Error: El rol especificado no existe.");
        }

        // Un administrador no puede quitarse a sí mismo el rol ni desactivar su
        // propia cuenta: se quedaría fuera del sistema sin poder recuperarlo.
        if (usuario.getId().equals(usuarioAutenticadoId()) && !rol.getId().equals(usuario.getRol().getId())) {
            throw new RuntimeException("Error: No puedes cambiar tu propio rol.");
        }

        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    /**
     * 6. Activa o desactiva una cuenta.
     *
     * Desactivar no borra nada: el usuario deja poder entrar a iniciar sesión,
     * pero sus datos siguen en el inventario y en el historial.
     */
    @Transactional
    public Usuario cambiarEstado(Long id, String estado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El usuario no existe."));

        String limpio = estado == null ? "" : estado.trim().toUpperCase();
        if (!ESTADO_ACTIVO.equals(limpio) && !ESTADO_INACTIVO.equals(limpio)) {
            throw new RuntimeException("Error: Estado no válido. Usa ACTIVO o INACTIVO.");
        }

        if (ESTADO_INACTIVO.equals(limpio) && usuario.getId().equals(usuarioAutenticadoId())) {
            throw new RuntimeException("Error: No puedes desactivar tu propia cuenta.");
        }

        usuario.setEstado(limpio);
        return usuarioRepository.save(usuario);
    }

    /**
     * Id del usuario que hace la petición, o `null` si no se pudo determinar.
     *
     * Se compara contra el propio usuario para impedir que el administrador se
     * quede sin acceso. El filtro JWT deja el correo como principal, así que de
     * ahí se saca el id. Cuando no hay sesión (por ejemplo en pruebas) la
     * comprobación simplemente no se aplica.
     */
    private Long usuarioAutenticadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof String correo)) {
            return null;
        }

        return usuarioRepository.findByCorreo(correo).map(Usuario::getId).orElse(null);
    }

    // 7. Método para listar los roles disponibles en el formulario de registro
    public List<Rol> obtenerRoles() {
        return rolRepository.findAll();
    }

    // 8. Eliminar un usuario permanentemente
    /**
     * Borra la cuenta y los datos personales que le pertenecen.
     *
     * El plan Premium es de la institución, no de la cuenta (vive en una sola
     * fila de `plan_institucion`), así que no hay una suscripción por usuario
     * que borrar aquí. Lo que sí se elimina es todo lo que apunta a la cuenta:
     *
     * - Fechas, fallas, historiales y licencias: se les quita la referencia para
     *   conservar el registro del inventario, que es compartido y no debe
     *   desaparecer con la persona.
     * - Préstamos ya devueltos o cancelados: se eliminan porque son historial
     *   exclusivo de esa cuenta. Si hay un préstamo activo se rechaza el borrado
     *   para no perder el rastro de un equipo que sigue fuera.
     * - Responsables de custodia creados por la cuenta: se eliminan si nada más
     *   los usa; si un equipo o un préstamo los referencia, se conservan sin el
     *   vínculo al usuario para no romper el inventario.
     *
     * Todo ocurre en una transacción: si una parte falla, no queda nada a medias.
     */
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: El usuario no existe."));

        // Impedir que el admin se borre a sí mismo.
        if (usuario.getId().equals(usuarioAutenticadoId())) {
            throw new RuntimeException("Error: No puedes eliminar tu propia cuenta.");
        }

        protegerAdministradores(usuario);

        // Se guardan los responsables antes de desvincularlos: después ya no se
        // puede saber cuáles pertenecían a esta cuenta.
        List<Long> responsables = entityManager
                .createQuery("select r.id from ResponsableCustodia r where r.usuario.id = :id", Long.class)
                .setParameter("id", id)
                .getResultList();

        desvincularHistoricos(id);
        eliminarPrestamos(id);

        // Si el responsable no lo usa ningún equipo, préstamo ni historial, es
        // información exclusiva de la cuenta y se borra. Si está compartido, se
        // conserva (ya sin usuario) para no dejar equipos sin responsable.
        for (Long responsableId : responsables) {
            if (!responsableEnUso(responsableId)) {
                entityManager.createQuery("delete from ResponsableCustodia r where r.id = :id")
                        .setParameter("id", responsableId)
                        .executeUpdate();
            }
        }

        usuarioRepository.delete(usuario);
        entityManager.flush();
    }

    /**
     * Evita que el sistema se quede sin una cuenta de administración.
     *
     * No basta con impedir el auto-borrado: otro administrador podría eliminar
     * la última cuenta ADMINISTRADOR y dejar el sistema sin nadie que gestione
     * cuentas y plan. Se exige que quede al menos otra cuenta con ese rol.
     */
    private void protegerAdministradores(Usuario usuario) {
        if (usuario.getRol() == null || !ROL_ADMINISTRADOR.equals(usuario.getRol().getNombre())) {
            return;
        }

        Long otrosAdmins = entityManager.createQuery(
                        "select count(u) from Usuario u where u.rol.nombre = :rol and u.id <> :id",
                        Long.class)
                .setParameter("rol", ROL_ADMINISTRADOR)
                .setParameter("id", usuario.getId())
                .getSingleResult();

        if (otrosAdmins == 0) {
            throw new RuntimeException(
                    "Error: No puedes eliminar la última cuenta de administrador. "
                            + "Crea o activa otra cuenta ADMINISTRADOR antes de borrar esta.");
        }

        if (ESTADO_ACTIVO.equals(usuario.getEstado())) {
            Long otrosAdminsActivos = entityManager.createQuery(
                            "select count(u) from Usuario u where u.rol.nombre = :rol "
                                    + "and u.estado = :estado and u.id <> :id",
                            Long.class)
                    .setParameter("rol", ROL_ADMINISTRADOR)
                    .setParameter("estado", ESTADO_ACTIVO)
                    .setParameter("id", usuario.getId())
                    .getSingleResult();

            if (otrosAdminsActivos == 0) {
                throw new RuntimeException(
                        "Error: Esta es la única cuenta ADMINISTRADOR activa. "
                                + "Activa otra cuenta de administrador antes de eliminarla.");
            }
        }
    }

    /**
     * Un préstamo activo ata un equipo a una persona: si se borrara la cuenta,
     * nadie sabría a quién reclamarle la devolución. Los préstamos ya cerrados
     * son historial de la cuenta y sí se eliminan con ella.
     */
    private void eliminarPrestamos(Long id) {
        Long activos = entityManager.createQuery(
                        "select count(p) from Prestamo p where p.usuarioPrestamo.id = :id "
                                + "and p.estado in ('PRESTADO', 'VENCIDO')",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult();

        if (activos > 0) {
            throw new RuntimeException(
                    "Error: La cuenta tiene " + activos
                            + " préstamo(s) activo(s). Registra la devolución antes de eliminarla.");
        }

        entityManager.createQuery("delete from Prestamo p where p.usuarioPrestamo.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    /** `true` si un responsable de custodia sigue referenciado por el inventario. */
    private boolean responsableEnUso(Long responsableId) {
        Long equipos = contar(
                "select count(e) from Equipo e where e.responsableCustodia.id = :id", responsableId);
        Long prestamos = contar(
                "select count(p) from Prestamo p where p.responsable.id = :id", responsableId);
        Long historial = contar(
                "select count(h) from HistorialCustodia h "
                        + "where h.responsableAnterior.id = :id or h.responsableNuevo.id = :id",
                responsableId);
        Long licencias = contar(
                "select count(l) from Licencia l where l.responsable.id = :id", responsableId);

        return equipos + prestamos + historial + licencias > 0;
    }

    private Long contar(String consulta, Long id) {
        return entityManager.createQuery(consulta, Long.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    /** Quita la referencia al usuario de los registros históricos que lo usan. */
    private void desvincularHistoricos(Long id) {
        actualizar("update Verificacion v set v.tecnico = null where v.tecnico.id = :id", id);
        actualizar("update Falla f set f.reportadoPor = null where f.reportadoPor.id = :id", id);
        actualizar("update Falla f set f.asignadoA = null where f.asignadoA.id = :id", id);
        actualizar("update HistorialFalla h set h.usuarioCambio = null where h.usuarioCambio.id = :id", id);
        actualizar("update HistorialCustodia h set h.usuarioQueAsigna = null where h.usuarioQueAsigna.id = :id", id);
        actualizar("update LicenciaEquipo l set l.instaladoPor = null where l.instaladoPor.id = :id", id);
        actualizar("update ResponsableCustodia r set r.usuario = null where r.usuario.id = :id", id);
        actualizar("update Prestamo p set p.usuarioEntrega = null where p.usuarioEntrega.id = :id", id);
    }

    private void actualizar(String consulta, Long id) {
        entityManager.createQuery(consulta)
                .setParameter("id", id)
                .executeUpdate();
    }

    /**
     * 9. Valida las credenciales y devuelve el token firmado.
     *
     * Las cuentas anteriores a este cambio guardaban la contraseña en texto
     * plano. Para no romperlas, si el hash almacenado no parece un BCrypt se
     * compara en claro y, si acierta, se re-cifra en el acto.
     */
    @Transactional
    public LoginResponse autenticar(String correo, String contrasena) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas."));

        verificarContrasena(usuario, contrasena);

        if (ESTADO_INACTIVO.equals(usuario.getEstado())) {
            throw new RuntimeException("Tu cuenta está desactivada. Contacta al administrador.");
        }

        Rol rol = usuario.getRol();
        String nombreRol = rol != null ? rol.getNombre() : ROL_DOCENTE;

        String token = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), nombreRol);

        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                nombreRol,
                usuario.getEstado()
        );
    }

    /** Lanza si la contraseña no corresponde, migrando a BCrypt si hace falta. */
    private void verificarContrasena(Usuario usuario, String contrasena) {
        String almacenada = usuario.getContrasena();

        if (almacenada == null || almacenada.isBlank()) {
            throw new RuntimeException("Credenciales incorrectas.");
        }

        if (esHashBcrypt(almacenada)) {
            if (!passwordEncoder.matches(contrasena, almacenada)) {
                throw new RuntimeException("Credenciales incorrectas.");
            }
            return;
        }

        // Cuenta heredada en texto plano: comparamos y aprovechamos para cifrar.
        if (!almacenada.equals(contrasena)) {
            throw new RuntimeException("Credenciales incorrectas.");
        }

        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuarioRepository.save(usuario);
    }

    /** Los hashes de BCrypt empiezan por $2a$, $2b$ o $2y$. */
    private boolean esHashBcrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
    }
}
