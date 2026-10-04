/**
 * Base del backend.
 *
 * Apunta al servicio desplegado en Render. Todo lo demás (API_USUARIOS,
 * API_SALAS, ...) se deriva de aquí, así que este es el único sitio donde hay
 * que cambiar la dirección para mover el backend.
 *
 * Sigue siendo una constante, pero ahora el valor por defecto es el que sirve
 * en producción. Para trabajar contra un backend local en el 8080, cambia esta
 * línea a 'http://localhost:8080/api' y recarga.
 */
export const API_BASE = 'https://sih-inventarios.onrender.com/api';

export const API_USUARIOS = `${API_BASE}/usuarios`;
export const API_SALAS = `${API_BASE}/salas`;
export const API_EQUIPOS = `${API_BASE}/equipos`;
export const API_FALLAS = `${API_BASE}/fallas`;
export const API_MANTENIMIENTOS = `${API_BASE}/mantenimientos`;
export const API_PLAN = `${API_BASE}/plan`;

/** Fotografía del inventario: totales por estado, por aula y listas de detalle. */
export const API_INVENTARIO = `${API_BASE}/inventario`;

/** Conteos físicos de las aulas, los hace el técnico o el administrador. */
export const API_VERIFICACIONES = `${API_BASE}/verificaciones`;
