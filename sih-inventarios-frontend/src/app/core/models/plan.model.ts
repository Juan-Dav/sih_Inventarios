/**
 * Estado del plan y reglas de los planes.
 *
 * El plan es de la institución, no del usuario: lo consultan y lo cambian los
 * tres paneles, porque los límites de inventario tienen que ser el mismo número
 * para el técnico, el docente y la administración. Comprar y cancelar se puede
 * desde cualquiera de ellos; la compra es simulada, no hay pasarela detrás.
 */

export const NOMBRES_PLAN = ['GRATUITO', 'PREMIUM'] as const;
export type NombrePlan = (typeof NOMBRES_PLAN)[number];

/** Lo que devuelve `GET /api/plan`, con el consumo ya calculado. */
export interface EstadoPlan {
  plan: NombrePlan;
  precio: number;
  /** Tope de aulas, o `null` cuando no hay límite. */
  maxSalas: number | null;
  /** Tope de equipos por sala, o `null` cuando no hay límite. */
  maxEquiposPorSala: number | null;
  /** El backend ya resuelve el `null` en estos dos, para no repetir el cálculo. */
  salasIlimitadas: boolean;
  equiposIlimitados: boolean;
  /**
 * `PENDIENTE` y `PAGADO` son los estados de una compra viva; `CANCELADO` deja
 * constancia de que hubo un Premium y se dio de baja, aunque el plan activo ya
 * sea el gratuito.
 */
estadoPago: 'PENDIENTE' | 'PAGADO' | 'CANCELADO';
  referenciaPago: string | null;
  fechaActivacion: string | null;
  salasRegistradas: number;
  equiposRegistrados: number;
}

/*
 * Precio y límites del plan.
 *
 * Se dejan como constantes solo para pintar algo antes de que llegue el GET, y
 * como valor por si la petición falla. En cuanto responde `GET /api/plan` se usa
 * lo que manda el backend, porque son los mismos números con los que él valida
 * los alta: si el administrador cambiara la configuración, el panel y la regla
 * seguirían siendo la misma cosa.
 */
export const PRECIO_PREMIUM = 150_000;
export const LIMITE_GRATUITO_SALAS = 2;
export const LIMITE_GRATUITO_EQUIPOS_POR_SALA = 12;

/**
 * Un usuario del catálogo de cuentas.
 *
 * `rol` no es opcional porque en la tabla la relación es obligatoria: la base de
 * datos no deja guardar una cuenta sin rol.
 */
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  estado: string;
  rol: { id: number; nombre: string };
}

/**
 * Texto y color del estado de una cuenta.
 *
 * Solo se usa dentro del panel del administrador, que va sobre el tema oscuro.
 * Por eso los textos son `txt-ok` / `txt-espera` / `txt-alerta` y no los
 * tokens `mint-700`, `sun-700` y `pop-600` de la escala del sitio: esos tres
 * están calculados para leerse sobre blanco y sobre el fondo oscuro del panel
 * no pasarían los 3:1.
 */
export const ESTILO_ESTADO_USUARIO: Record<string, { texto: string; clases: string }> = {
  ACTIVO: {
    texto: 'Activo',
    clases: 'bg-mint-400/15 txt-ok ring-1 ring-inset ring-mint-400/30',
  },
  INACTIVO: {
    texto: 'Inactivo',
    clases: 'bg-pop-500/15 txt-alerta ring-1 ring-inset ring-pop-500/30',
  },
  PENDIENTE: {
    texto: 'Pendiente',
    clases: 'bg-sun-400/15 txt-espera ring-1 ring-inset ring-sun-400/30',
  },
};

/**
 * Formatea pesos sin decimales: el precio se muestra como "$150.000" y no como
 * "$150.000,00", porque es un monto entero.
 */
export function formatearPesos(valor: number): string {
  return `$${valor.toLocaleString('es-CO')}`;
}