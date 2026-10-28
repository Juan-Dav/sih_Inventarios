/**
 * Base del backend.
 *
 * En producción apunta al servicio desplegado en Render. Todo lo demás
 * (API_USUARIOS, API_SALAS, ...) se deriva de aquí, así que este es el único
 * sitio donde hay que cambiar la dirección para mover el backend.
 *
 * Al trabajar en local (`ng serve` en localhost) se usa el backend del 8080:
 * así se puede probar un cambio del backend sin desplegarlo primero. La
 * detección es por el host del navegador, no por una constante que haya que
 * cambiar a mano y luego olvidar revertir.
 */
const HOSTS_LOCALES = ['localhost', '127.0.0.1', '[::1]'];

const esLocal = typeof window !== 'undefined' && HOSTS_LOCALES.includes(window.location.hostname);

export const API_BASE = esLocal
  ? 'http://localhost:8080/api'
  : 'https://sih-inventarios.onrender.com/api';

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

/** Reportes PDF/Excel. */
export const API_REPORTES = `${API_BASE}/reportes`;
