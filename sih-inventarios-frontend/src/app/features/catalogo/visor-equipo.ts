import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { API_BASE } from '../../core/config/api';
import { formatearFecha, nombreEquipo } from '../../core/models/inventario.model';

/** Datos completos que devuelve `GET /api/consulta/qr/{codigo}`. */
interface FichaEquipo {
  id: number;
  codigo: string;
  codigoQr?: string | null;
  nombreEquipo?: string | null;
  categoria?: string | null;
  caracteristicas?: string | null;
  marca?: string | null;
  modelo?: string | null;
  numeroSerie?: string | null;
  fechaAdquisicion?: string | null;
  garantiaHasta?: string | null;
  ubicacionDetalle?: string | null;
  observaciones?: string | null;
  activo?: boolean | null;
  estado?: string | null;
  sala?: string | null;
  area?: string | null;
  departamento?: string | null;
}

/** Texto y color con el que se pinta el estado del equipo. */
interface EstadoFicha {
  clave: string;
  texto: string;
  descripcion: string;
}

/**
 * Ficha pública que se abre al escanear el QR de un equipo.
 *
 * No pide sesión: la persona que escanea puede ser un docente, un técnico o
 * alguien de la institución con el celular en la mano. La pantalla está pensada
 * primero para ese teléfono, y se refresca sola para que el estado que se vea
 * sea el actual aunque el equipo haya pasado de fuera de servicio a operativo
 * después de imprimir la etiqueta.
 */
@Component({
  selector: 'app-visor-equipo',
  templateUrl: './visor-equipo.html',
  styleUrl: './visor-equipo.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VisorEquipo implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /** Cada cuánto se vuelve a consultar el estado mientras la ficha está visible. */
  private static readonly REFRESCO_MS = 20_000;

  protected readonly equipo = signal<FichaEquipo | null>(null);
  protected readonly cargando = signal(true);
  protected readonly actualizando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly ultimaActualizacion = signal<Date | null>(null);

  private codigo = '';
  private enVuelo = false;

  /** Nombre legible del equipo, con marca y modelo cuando están registrados. */
  protected readonly nombre = computed(() => {
    const equipo = this.equipo();
    if (!equipo) return 'Equipo';
    return (
      equipo.nombreEquipo?.trim() ||
      nombreEquipo({
        marca: equipo.marca ?? null,
        modelo: equipo.modelo ?? null,
        caracteristicas: equipo.caracteristicas ?? null,
      })
    );
  });

  /** Sala, área y departamento, sin repetir separadores vacíos. */
  protected readonly lugar = computed(() => {
    const equipo = this.equipo();
    if (!equipo) return 'Sin ubicación registrada';

    const partes = [equipo.sala, equipo.area, equipo.departamento].filter(
      (parte): parte is string => Boolean(parte && parte.trim()),
    );

    return partes.length > 0 ? partes.join(' · ') : 'Sin ubicación registrada';
  });

  protected readonly estado = computed<EstadoFicha>(() => {
    switch ((this.equipo()?.estado ?? '').trim().toUpperCase()) {
      case 'OPERATIVO':
        return {
          clave: 'OPERATIVO',
          texto: 'Operativo',
          descripcion: 'El equipo está funcionando y disponible para su uso.',
        };
      case 'EN_MANTENIMIENTO':
        return {
          clave: 'EN_MANTENIMIENTO',
          texto: 'En mantenimiento',
          descripcion: 'El equipo está en revisión por el equipo técnico.',
        };
      case 'FUERA_DE_SERVICIO':
        return {
          clave: 'FUERA_DE_SERVICIO',
          texto: 'Fuera de servicio',
          descripcion: 'El equipo no está disponible; requiere una intervención técnica.',
        };
      default: {
        // Si el inventario guarda un estado nuevo, se muestra tal cual en vez
        // de esconderlo detrás de "sin estado". Solo se cambian los guiones
        // bajos por espacios para que se lea como una etiqueta.
        const valor = (this.equipo()?.estado ?? '').trim();
        const texto = valor
          ? valor.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letra) => letra.toUpperCase())
          : 'Sin estado registrado';

        return {
          clave: 'OTRO',
          texto,
          descripcion: valor
            ? 'El inventario reporta este estado para el equipo.'
            : 'El inventario no tiene un estado registrado para este equipo.',
        };
      }
    }
  });

  protected readonly horaActualizacion = computed(() => {
    const fecha = this.ultimaActualizacion();
    if (!fecha) return '—';

    return fecha.toLocaleTimeString('es-CO', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  });

  constructor() {
    // El refresco automático solo existe en el navegador: en el render del
    // servidor no hay pestaña visible ni temporizadores que tengan sentido.
    afterNextRender(() => {
      const refrescarSiVisible = () => {
        if (document.visibilityState === 'visible') {
          this.cargar(true);
        }
      };

      const intervalo = window.setInterval(refrescarSiVisible, VisorEquipo.REFRESCO_MS);

      // Al volver a la pestaña después de escanear de nuevo, se consulta en el
      // acto en lugar de esperar hasta el siguiente intervalo.
      document.addEventListener('visibilitychange', refrescarSiVisible);
      window.addEventListener('focus', refrescarSiVisible);

      this.destroyRef.onDestroy(() => {
        window.clearInterval(intervalo);
        document.removeEventListener('visibilitychange', refrescarSiVisible);
        window.removeEventListener('focus', refrescarSiVisible);
      });
    });
  }

  ngOnInit(): void {
    this.codigo = this.route.snapshot.paramMap.get('codigo')?.trim() ?? '';

    if (!this.codigo) {
      this.cargando.set(false);
      this.error.set('El enlace no trae el código de un equipo. Vuelve a escanear el QR.');
      return;
    }

    this.cargar(false);
  }

  /** Vuelve a pedir la ficha; el botón de la pantalla también entra por aquí. */
  protected refrescar(): void {
    this.cargar(true);
  }

  protected fecha(valor: string | null | undefined): string {
    return valor ? formatearFecha(valor) : 'No especificada';
  }

  /** Fechas de calendario (`YYYY-MM-DD`) sin desplazarlas por la zona horaria. */
  protected dia(valor: string | null | undefined): string {
    if (!valor) return 'No especificada';

    const fecha = new Date(`${valor}T00:00:00`);
    if (Number.isNaN(fecha.getTime())) return formatearFecha(valor);

    return fecha.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  protected texto(valor: string | null | undefined): string {
    const limpio = valor?.trim();
    return limpio ? limpio : 'No especificado';
  }

  private cargar(silencioso: boolean): void {
    if (this.enVuelo) return;

    this.enVuelo = true;
    this.error.set(null);

    if (silencioso) {
      this.actualizando.set(true);
    } else {
      this.cargando.set(true);
    }

    this.http
      .get<FichaEquipo>(`${API_BASE}/consulta/qr/${encodeURIComponent(this.codigo)}`)
      .subscribe({
        next: (ficha) => {
          this.enVuelo = false;
          this.equipo.set(ficha);
          this.ultimaActualizacion.set(new Date());
          this.cargando.set(false);
          this.actualizando.set(false);
        },
        error: (respuesta: unknown) => {
          this.enVuelo = false;
          this.cargando.set(false);
          this.actualizando.set(false);
          this.error.set(this.mensajeError(respuesta));
        },
      });
  }

  private mensajeError(respuesta: unknown): string {
    if (respuesta instanceof HttpErrorResponse) {
      if (respuesta.status === 404) {
        return 'No encontramos ningún equipo con este código QR. Puede que la etiqueta ya no corresponda a un equipo del inventario.';
      }

      if (respuesta.status === 0) {
        return 'No hay conexión con el servidor. Revisa el internet del celular e inténtalo de nuevo.';
      }
    }

    return 'No se pudo consultar la ficha del equipo. Inténtalo de nuevo en unos segundos.';
  }
}
