/**
 * Base del backend.
 *
 * Sigue siendo una constante hardcodeada (el propio código ya lo lleva un TODO
 * desde antes): lo ideal es moverla a un environment para poder cambiar de
 * servidor sin recompilar.
 */
export const API_BASE = 'http://localhost:8080/api';

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
