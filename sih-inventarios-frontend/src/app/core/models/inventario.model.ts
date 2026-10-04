/**
 * Formas del backend para salas, equipos, fallas y mantenimientos.
 *
 * Reflejan las entidades JPA tal y como las serializa Spring: los objetos
 * anidados vienen enteros (un equipo trae su `sala`, una falla trae su
 * `equipo`), así que aquí no hay que aplanar nada.
 */

export interface Sala {
  id: number;
  nombre: string;
  capacidadMaxima: number;
}

export const ESTADOS_EQUIPO = ['OPERATIVO', 'EN_MANTENIMIENTO', 'FUERA_DE_SERVICIO'] as const;
export type EstadoEquipo = (typeof ESTADOS_EQUIPO)[number];

export interface Equipo {
  id: number;
  codigo: string;
  caracteristicas: string | null;
  /** Marca del equipo; el backend la dejó opcional para no romper los datos ya cargados. */
  marca?: string | null;
  modelo?: string | null;
  estado: EstadoEquipo;
  sala: Sala;
}

export const ESTADOS_FALLA = ['PENDIENTE', 'EN_REVISION', 'RESUELTO'] as const;
export type EstadoFalla = (typeof ESTADOS_FALLA)[number];

export interface Falla {
  id: number;
  descripcion: string;
  estado: EstadoFalla;
  fechaReporte: string;
  equipo: Equipo;
}

export interface Mantenimiento {
  id: number;
  diagnostico: string;
  solucion: string;
  fechaMantenimiento: string;
  equipo: Equipo;
  falla: Falla | null;
}

/** Cuerpo para crear una sala. */
export interface NuevaSala {
  nombre: string;
  capacidadMaxima: number;
}

/** Cuerpo para editar una sala. */
export interface EditarSala {
  nombre: string;
  capacidadMaxima: number;
}

/**
 * Cuerpo para editar un equipo.
 *
 * La sala de destino viaja como `?idSala=`; si se omite, el equipo se queda en
 * la que ya tenía.
 */
export interface EditarEquipo {
  codigo: string;
  caracteristicas: string | null;
  marca?: string | null;
  modelo?: string | null;
  estado: EstadoEquipo;
}

/** Fallas y mantenimientos de un equipo, tal como los devuelve el historial. */
export interface HistorialEquipo {
  equipo: Equipo;
  fallas: Falla[];
  mantenimientos: Mantenimiento[];
}

/**
 * Cuerpo para crear un equipo; la sala viaja como `?idSala=`.
 *
 * `marca` y `modelo` son opcionales porque en una demo rápida se escriben muy
 * pocas veces y no deben frenar el alta.
 */
export interface NuevoEquipo {
  codigo: string;
  caracteristicas: string | null;
  marca?: string | null;
  modelo?: string | null;
  estado: EstadoEquipo;
}

/** Cuerpo para reportar una falla; el equipo viaja como `?idEquipo=`. */
export interface NuevaFalla {
  descripcion: string;
  estado: EstadoFalla;
}

/**
 * Cuerpo para registrar un mantenimiento.
 *
 * El backend exige `?idEquipo=` y `?idFalla=`, así que la relación se manda en
 * la query y no en el cuerpo.
 */
export interface NuevoMantenimiento {
  diagnostico: string;
  solucion: string;
}

/** Texto y color de cada estado de equipo, para pintar las etiquetas. */
export const ESTILO_EQUIPO: Record<EstadoEquipo, { texto: string; clases: string }> = {
  OPERATIVO: {
    texto: 'Operativo',
    clases: 'bg-mint-400/15 text-mint-400 ring-1 ring-inset ring-mint-400/30',
  },
  EN_MANTENIMIENTO: {
    texto: 'En mantenimiento',
    clases: 'bg-sun-400/15 text-sun-400 ring-1 ring-inset ring-sun-400/30',
  },
  FUERA_DE_SERVICIO: {
    texto: 'Fuera de servicio',
    clases: 'bg-pop-500/15 text-pop-400 ring-1 ring-inset ring-pop-500/30',
  },
};

/** Texto y color de cada estado de falla. */
export const ESTILO_FALLA: Record<EstadoFalla, { texto: string; clases: string }> = {
  PENDIENTE: {
    texto: 'Pendiente',
    clases: 'bg-sun-400/15 text-sun-400 ring-1 ring-inset ring-sun-400/30',
  },
  EN_REVISION: {
    texto: 'En revisión',
    clases: 'bg-aqua-400/15 text-aqua-400 ring-1 ring-inset ring-aqua-400/30',
  },
  RESUELTO: {
    texto: 'Resuelto',
    clases: 'bg-mint-400/15 text-mint-400 ring-1 ring-inset ring-mint-400/30',
  },
};

/**
 * Estilo de un estado de equipo.
 *
 * Es función y no acceso directo al `Record` porque la respuesta del backend
 * llega como `string`: si algún día trae un estado que no conozco, esta forma
 * devuelve el estilo neutro en vez de reventar la plantilla.
 */
export function estiloDeEquipo(
  estado: string | null | undefined,
): { texto: string; clases: string } {
  return (
    ESTILO_EQUIPO[estado as EstadoEquipo] ?? {
      texto: 'Sin estado',
      clases: 'bg-ink-100 text-ink-500 ring-1 ring-inset ring-ink-200',
    }
  );
}

/** Igual que `estiloDeEquipo`, pero para el estado de una falla. */
export function estiloDeFalla(estado: string | null | undefined): { texto: string; clases: string } {
  return (
    ESTILO_FALLA[estado as EstadoFalla] ?? {
      texto: 'Sin estado',
      clases: 'bg-ink-100 text-ink-500 ring-1 ring-inset ring-ink-200',
    }
  );
}

/**
 * Cómo se escribe un equipo en las tablas: marca y modelo.
 *
 * Marca y modelo llegaron después que el resto del inventario, así que los
 * equipos dados de alta antes las tienen a `null`. Mostrar solo el código dejaba
 * esas filas sin nada que distinguir entre sí, que es justo lo que el técnico
 * necesita para saber qué máquina está mirando. Por eso el recorrido cae por
 * `caracteristicas` y, en último caso, dice que falta registrarla en vez de
 * imprimir un guion suelto.
 */
export function nombreEquipo(
  equipo: Pick<Equipo, 'marca' | 'modelo' | 'caracteristicas'> | null | undefined,
): string {
  if (!equipo) return 'Equipo sin datos';

  const marca = equipo.marca?.trim();
  const modelo = equipo.modelo?.trim();

  if (marca && modelo) return `${marca} ${modelo}`;
  if (marca) return marca;
  if (modelo) return modelo;

  const caracteristicas = equipo.caracteristicas?.trim();
  if (caracteristicas) return caracteristicas;

  return 'Sin marca ni modelo';
}

/**
 * Cómo está una sala, tal y como lo devuelve el resumen del backend.
 *
 * Ojo con los nombres: aquí la sala es `salaId` y su nombre `sala`, porque el
 * backend reutiliza esta fila también en `/api/verificaciones`, donde el nombre
 * viene suelto. No es lo mismo que un `Sala`.
 */
export interface ConteoSala {
  salaId: number;
  /** Nombre de la aula. */
  sala: string;
  capacidadMaxima: number;
  equiposRegistrados: number;
  equiposOperativos: number;
  equiposEnMantenimiento: number;
  equiposFueraDeServicio: number;
  /** Fecha del último conteo físico, o `null` si la sala nunca se recorrió. */
  ultimaVerificacion: string | null;
  /** Diferencia del último conteo, o `null` si nunca se recorrió. */
  equiposFaltantes: number | null;
}

/** Fotografía completa del inventario que ve el administrador. */
export interface ResumenInventario {
  totalSalas: number;
  totalEquipos: number;
  /** Suma de las capacidades declaradas de todas las aulas. */
  capacidadTotal: number;
  equiposOperativos: number;
  equiposEnMantenimiento: number;
  equiposFueraDeServicio: number;
  totalFallas: number;
  fallasPendientes: number;
  fallasEnRevision: number;
  fallasResueltas: number;
  totalMantenimientos: number;
  totalVerificaciones: number;
  salas: ConteoSala[];
  listaEquiposFueraDeServicio: Equipo[];
  listaEquiposEnMantenimiento: Equipo[];
  /** Fallas sin resolver, de la más antigua a la más reciente. */
  fallasAbiertas: Falla[];
}

/**
 * Conteo físico de una sala.
 *
 * `cantidadEsperada` nunca la manda el frontend: la calcula el backend con los
 * equipos registrados, para que un conteo no pueda "encontrar" un número falso.
 * `cantidadFaltante` y `completa` tampoco se piden; los dos salen de la misma
 * entidad como getters, y por eso no llegan en el cuerpo que se envía al POST.
 */
export interface Verificacion {
  id: number;
  cantidadEsperada: number;
  cantidadEncontrada: number;
  /** Getter del backend: nunca negativo. */
  cantidadFaltante: number;
  /** Getter del backend. */
  completa: boolean;
  /** Códigos de los que faltaron, separados por coma; `null` si no se anotaron. */
  equiposFaltantes: string | null;
  /**
   * Códigos de los que estaban pero ya no sirven, separados por coma.
   *
   * Un conteo puede cuadrar y aun así haber equipos quemados: el número no lo
   * cuenta. Por eso van aparte de las observaciones, que son texto libre.
   */
  equiposDanados: string | null;
  observaciones: string | null;
  fechaVerificacion: string;
  sala: Sala;
  /** Quién hizo el recorrido. El backend lo deja en `null` si no reconoce el token. */
  tecnico: { id: number; nombre: string } | null;
}

/** Cuerpo para registrar una verificación; la sala viaja como `?idSala=`. */
export interface NuevaVerificacion {
  cantidadEncontrada: number;
  equiposFaltantes?: string | null;
  equiposDanados?: string | null;
  observaciones?: string | null;
}

/**
 * Estado de verificación de una sala, tal y como lo devuelve
 * `GET /api/verificaciones`.
 *
 * Es lo que usa el técnico para recorrer: de un golpe trae lo registrado, el
 * desglose por estado, la lista de equipos y el último conteo, sin tener que
 * cruzar cuatro peticiones para una sola sala.
 */
export interface EstadoVerificacionSala {
  salaId: number;
  sala: string;
  capacidadMaxima: number;
  equiposRegistrados: number;
  equiposOperativos: number;
  equiposEnMantenimiento: number;
  equiposFueraDeServicio: number;
  verificaciones: Verificacion[];
  ultimaVerificacion: Verificacion | null;
  equipos: Equipo[];
  /** Getter del backend: diferencia del último conteo, o `null` si nunca se recorrió. */
  equiposFaltantes: number | null;
  /** Getter del backend: `true` cuando ya se hizo al menos un conteo. */
  verificada: boolean;
}

/** `2026-09-30T04:12:33` -> `30 sep 2026, 04:12` */
export function formatearFecha(iso: string | null | undefined): string {
  if (!iso) return 'Sin fecha';

  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return 'Sin fecha';

  return fecha.toLocaleString('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
