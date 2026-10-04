import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_PLAN, API_USUARIOS } from '../config/api';
import { Usuario, EstadoPlan } from '../models/plan.model';
import { RolCatalogo } from './roles.service';

/**
 * Plan contratado y cuentas de usuario.
 *
 * Son las dos cosas que solo manage el administrador y que por eso viven juntas:
 * el plan decide cuánto inventario cabe, y las cuentas deciden quién lo opera.
 */
@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);

  private readonly _plan = signal<EstadoPlan | null>(null);
  private readonly _usuarios = signal<Usuario[]>([]);

  private readonly _cargandoPlan = signal(false);
  private readonly _cargandoUsuarios = signal(false);

  readonly plan = this._plan.asReadonly();
  readonly usuarios = this._usuarios.asReadonly();
  readonly cargandoPlan = this._cargandoPlan.asReadonly();
  readonly cargandoUsuarios = this._cargandoUsuarios.asReadonly();

  /**
   * `true` mientras el plan gratuito siga vigente.
   *
   * Se calcula en vez de guardarse para que no se pueda desincronizar del plan
   * real: si el nombre dice PREMIUM pero el pago quedó pendiente, el sistema
   * sigue tratándose como gratuito.
   */
  readonly esGratuito = computed(() => this._plan()?.plan !== 'PREMIUM');

  /** Salas que aún se pueden registrar según el plan. */
  readonly salasRestantes = computed(() => {
    const plan = this._plan();
    if (!plan || plan.salasIlimitadas) return 0;

    return Math.max(0, (plan.maxSalas ?? 0) - plan.salasRegistradas);
  });

  /** `true` cuando ya no queda ninguna sala libre en el plan gratuito. */
  readonly alcanzoLimiteSalas = computed(() => this.salasRestantes() === 0);

  constructor() {
    // Se pide nada más entrar y no en cada pantalla, porque el plan no cambia
    // solo: lo cambia el administrador comprándolo desde su panel.
    this.cargarPlan();
  }

  /* ------------------------------- Plan ------------------------------- */

  cargarPlan(): void {
    this._cargandoPlan.set(true);

    this.http.get<EstadoPlan>(API_PLAN).subscribe({
      next: (plan) => {
        this._plan.set(plan);
        this._cargandoPlan.set(false);
      },
      // Un fallo aquí no se muestra al usuario: el plan solo ajusta textos y
      // barras de progreso, y las altas las valida el backend igual. Los `null`
      // de `plan()` tienen su valor por defecto para que la pantalla siga legible.
      error: () => this._cargandoPlan.set(false),
    });
  }

  /**
   * Compra simulada del plan Premium.
   *
   * No hay pasarela de pago: el backend marca el plan como pagado y devuelve
   * el estado nuevo, así que solo hay que refrescar los valores que dependan de
   * los límites.
   *
   * Se manda `{}` y no `null`: aunque no hay cuerpo que procesar, Angular
   * serializaría `null` como cuerpo vacío y algunas versiones del backend lo
   * rechazan al no poder leer JSON.
   */
  comprarPremium(): Observable<EstadoPlan> {
    return this.http.post<EstadoPlan>(`${API_PLAN}/comprar`, {}).pipe(
      tap((plan) => this._plan.set(plan)),
    );
  }

  /**
   * Cancela el Premium y vuelve al plan gratuito.
   *
   * Mismo criterio que la compra: no se borra nada del inventario, solo se
   * devuelven los topes, y quien llama se encarga de recargar lo que dependía de
   * ellos para que los avisos de límite no queden desfasados.
   */
  cancelarPremium(): Observable<EstadoPlan> {
    return this.http.post<EstadoPlan>(`${API_PLAN}/cancelar`, {}).pipe(
      tap((plan) => this._plan.set(plan)),
    );
  }

  /* ------------------------------ Usuarios ------------------------------ */

  cargarUsuarios(): void {
    this._cargandoUsuarios.set(true);

    this.http.get<Usuario[]>(API_USUARIOS).subscribe({
      next: (usuarios) => {
        this._usuarios.set(usuarios);
        this._cargandoUsuarios.set(false);
      },
      // El error real lo pinta quien llama; aquí solo se suelta el estado de
      // carga para que el botón "Actualizar" no se quede pulsado para siempre.
      error: () => this._cargandoUsuarios.set(false),
    });
  }

  /** Crea la cuenta con el rol que elija el administrador. */
  crearUsuario(datos: { nombre: string; correo: string; contrasena: string }, rol: string): Observable<Usuario> {
    return this.http
      .post<Usuario>(`${API_USUARIOS}/registro`, datos, { params: { rol } })
      .pipe(tap((nuevo) => this._usuarios.update((lista) => [...lista, nuevo])));
  }

  /** Cambia nombre y correo. */
  actualizarUsuario(id: number, cambios: { nombre: string; correo: string }): Observable<Usuario> {
    return this.http
      .put<Usuario>(`${API_USUARIOS}/${id}`, cambios)
      .pipe(tap((actualizado) => this.reemplazar(actualizado)));
  }

  /** Asigna un rol nuevo a la cuenta. */
  asignarRol(id: number, rol: string): Observable<Usuario> {
    return this.http
      .patch<Usuario>(`${API_USUARIOS}/${id}/rol`, null, { params: { rol } })
      .pipe(tap((actualizado) => this.reemplazar(actualizado)));
  }

  /** Activa o desactiva la cuenta. */
  cambiarEstado(id: number, estado: string): Observable<Usuario> {
    return this.http
      .patch<Usuario>(`${API_USUARIOS}/${id}/estado`, null, { params: { estado } })
      .pipe(tap((actualizado) => this.reemplazar(actualizado)));
  }

  /** Sustituye la cuenta editada dentro de la lista ya cargada. */
  private reemplazar(actualizado: Usuario): void {
    this._usuarios.update((lista) =>
      lista.map((usuario) => (usuario.id === actualizado.id ? actualizado : usuario)),
    );
  }
}

/** Tipos reexportados para no obligar a los componentes a mirar dos archivos. */
export type { EstadoPlan, Usuario, RolCatalogo };