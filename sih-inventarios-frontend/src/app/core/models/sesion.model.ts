/** Roles que maneja el sistema. Deben coincidir con la tabla `roles`. */
export type Rol = 'ADMINISTRADOR' | 'TECNICO' | 'DOCENTE';

/** Usuario autenticado, reconstruido a partir del token. */
export interface UsuarioSesion {
  id: number;
  nombre: string;
  correo: string;
  rol: Rol;
  estado: string;
}

/** Respuesta de POST /api/usuarios/login. */
export interface LoginResponse {
  token: string;
  id: number;
  nombre: string;
  correo: string;
  rol: Rol;
  estado: string;
}

/** Dashboard al que se redirige según el rol. */
export const RUTA_POR_ROL: Record<Rol, string> = {
  ADMINISTRADOR: '/dashboard-admin',
  TECNICO: '/dashboard-tecnico',
  DOCENTE: '/dashboard-docente',
};

/** Nombre legible del rol, para mostrarlo en la interfaz. */
export const ETIQUETA_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  TECNICO: 'Técnico',
  DOCENTE: 'Docente',
};

/**
 * Roles que el registro público ofrece elegir.
 *
 * Se escriben aquí y no se piden al catálogo (`GET /api/usuarios/roles`) porque
 * son los mismos tres que valida el backend: un desplegable que dependiera de
 * la red dejaría el formulario de alta sin opciones —y por tanto el registro
 * bloqueado— cada vez que esa petición fallara. El catálogo sí se usa en el
 * panel del administrador, que solo se abre con sesión iniciada.
 */
export const ROLES_REGISTRO: readonly Rol[] = ['ADMINISTRADOR', 'TECNICO', 'DOCENTE'];
