import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, tap } from 'rxjs';

import { API_EQUIPOS, API_FALLAS, API_INVENTARIO, API_MANTENIMIENTOS, API_SALAS, API_VERIFICACIONES } from '../config/api';
import {
  EditarEquipo,
  EditarSala,
  Equipo,
  EstadoEquipo,
  EstadoFalla,
  EstadoVerificacionSala,
  Falla,
  HistorialEquipo,
  Mantenimiento,
  NuevaFalla,
  NuevaSala,
  NuevaVerificacion,
  NuevoEquipo,
  NuevoMantenimiento,
  ResumenInventario,
  Sala,
  Verificacion,
} from '../models/inventario.model';

/**
 * Puente con los cuatro módulos de inventario.
 *
 * Las listas se guardan en signals, así que cualquier pantalla que las lea se
 * actualiza sola en cuanto hay un alta, sin recargar ni pedir datos otra vez.
 *
 * No hay borrado en ningún sitio: el inventario es un registro histórico y
 * sacar un equipo de la lista borraría también sus fallas y sus mantenimientos.
 * Lo que sí se puede es editarlo y cambiar su estado.
 */
@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);

  private readonly _salas = signal<Sala[]>([]);
  private readonly _equipos = signal<Equipo[]>([]);
  private readonly _fallas = signal<Falla[]>([]);
  private readonly _mantenimientos = signal<Mantenimiento[]>([]);

  readonly salas = this._salas.asReadonly();
  readonly equipos = this._equipos.asReadonly();
  readonly fallas = this._fallas.asReadonly();
  readonly mantenimientos = this._mantenimientos.asReadonly();

  /** Marca que hay una carga en curso, para pintar el estado de los botones. */
  private readonly _cargando = signal(false);
  readonly cargando = this._cargando.asReadonly();

  /** Aviso de la última operación, para mostrarlo junto al formulario. */
  private readonly _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  /* -------------------- Indicadores para las tarjetas -------------------- */

  readonly totalSalas = computed(() => this._salas().length);

  readonly capacidadTotal = computed(() =>
    this._salas().reduce((total, sala) => total + (sala.capacidadMaxima ?? 0), 0),
  );

  readonly totalEquipos = computed(() => this._equipos().length);

  readonly equiposOperativos = computed(
    () => this._equipos().filter((equipo) => equipo.estado === 'OPERATIVO').length,
  );

  readonly totalFallas = computed(() => this._fallas().length);

  readonly fallasAbiertas = computed(
    () => this._fallas().filter((falla) => falla.estado !== 'RESUELTO').length,
  );

  readonly totalMantenimientos = computed(() => this._mantenimientos().length);

  /* ------------------------------ Lecturas ------------------------------ */

  listarSalas(): Observable<Sala[]> {
    return this.http.get<Sala[]>(API_SALAS).pipe(tap((salas) => this._salas.set(salas)));
  }

  listarEquipos(): Observable<Equipo[]> {
    return this.http
      .get<Equipo[]>(API_EQUIPOS)
      .pipe(tap((equipos) => this._equipos.set(equipos)));
  }

  listarFallas(): Observable<Falla[]> {
    return this.http.get<Falla[]>(API_FALLAS).pipe(tap((fallas) => this._fallas.set(fallas)));
  }

  listarMantenimientos(): Observable<Mantenimiento[]> {
    return this.http
      .get<Mantenimiento[]>(API_MANTENIMIENTOS)
      .pipe(tap((mantenimientos) => this._mantenimientos.set(mantenimientos)));
  }

  /** Trae las cuatro listas de una vez, que es lo que necesitan los paneles. */
  cargarTodo(): void {
    this._cargando.set(true);
    this.limpiarError();

    forkJoin({
      salas: this.listarSalas(),
      equipos: this.listarEquipos(),
      fallas: this.listarFallas(),
      mantenimientos: this.listarMantenimientos(),
    }).subscribe({
      next: () => this._cargando.set(false),
      error: (error: unknown) => {
        this._cargando.set(false);
        this._error.set(this.leerError(error));
      },
    });
  }

  /* --------------------- Lecturas para el inventario --------------------- */

  /**
   * Totales del inventario de toda la institución.
   *
   * El backend hace los `count` por estado y por aula en una sola consulta de
   * cada tipo. Es más barato que traer todas las filas y contarlas en el
   * navegador, y sobre todo no se desactualiza: la vista del administrador
   * siempre cuadra con la base.
   */
  obtenerResumen(): Observable<ResumenInventario> {
    return this.http.get<ResumenInventario>(`${API_INVENTARIO}/resumen`);
  }

  /** Equipos de un aula; se piden al elegir la sala en el formulario. */
  listarEquiposPorSala(idSala: number): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${API_EQUIPOS}/sala/${idSala}`);
  }

  listarEquiposPorEstado(estado: EstadoEquipo): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${API_EQUIPOS}/estado/${estado}`);
  }

  /** Los equipos que el técnico marcó como fuera de servicio. */
  listarEquiposFueraDeServicio(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${API_EQUIPOS}/fuera-de-servicio`);
  }

  listarFallasPorEstado(estado: EstadoFalla): Observable<Falla[]> {
    return this.http.get<Falla[]>(`${API_FALLAS}/estado/${estado}`);
  }

  /** Fallas sin resolver, de la más antigua a la más reciente. */
  listarFallasAbiertas(): Observable<Falla[]> {
    return this.http.get<Falla[]>(`${API_FALLAS}/abiertas`);
  }

  listarMantenimientosPorEquipo(idEquipo: number): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(`${API_MANTENIMIENTOS}/equipo/${idEquipo}`);
  }

  /* ----------------------- Conteos físicos del técnico ----------------------- */

  /**
   * Estado de verificación de todas las salas.
   *
   * Trae por aula lo registrado, el desglose por estado, los equipos y el
   * último conteo. Es lo que el técnico necesita para recorrer, y el
   * administrador para ver si alguna aula quedó sin contar.
   */
  listarEstadoPorSala(): Observable<EstadoVerificacionSala[]> {
    return this.http.get<EstadoVerificacionSala[]>(API_VERIFICACIONES);
  }

  /** Historial completo de conteos, de la más reciente a la más antigua. */
  listarHistorialVerificaciones(): Observable<Verificacion[]> {
    return this.http.get<Verificacion[]>(`${API_VERIFICACIONES}/historial`);
  }

  /** Conteos de una sala concreta. */
  listarVerificacionesDeSala(idSala: number): Observable<Verificacion[]> {
    return this.http.get<Verificacion[]>(`${API_VERIFICACIONES}/sala/${idSala}`);
  }

  registrarVerificacion(datos: NuevaVerificacion, idSala: number): Observable<Verificacion> {
    const params = new HttpParams().set('idSala', idSala);
    return this.http.post<Verificacion>(API_VERIFICACIONES, datos, { params });
  }

  /* ------------------------------- Altas ------------------------------- */

  /** Cambia el nombre o la capacidad de un aula. */
  actualizarSala(id: number, cambios: EditarSala): Observable<Sala> {
    return this.http.put<Sala>(`${API_SALAS}/${id}`, cambios).pipe(
      tap((actualizada) =>
        this._salas.update((lista) =>
          lista.map((sala) => (sala.id === actualizada.id ? actualizada : sala)),
        ),
      ),
    );
  }

  /**
   * Edita un equipo.
   *
   * `idSala` es opcional: si se manda, el equipo se traslada a esa aula y el
   * backend vuelve a validar los límites del destino.
   */
  actualizarEquipo(id: number, cambios: EditarEquipo, idSala?: number | null): Observable<Equipo> {
    const params = idSala ? new HttpParams().set('idSala', idSala) : undefined;

    return this.http.put<Equipo>(`${API_EQUIPOS}/${id}`, cambios, { params }).pipe(
      tap((actualizado) =>
        this._equipos.update((lista) =>
          lista.map((equipo) => (equipo.id === actualizado.id ? actualizado : equipo)),
        ),
      ),
    );
  }

  /**
   * Refleja en las listas locales lo que el backend ya hizo por su cuenta.
   *
   * Registrar un mantenimiento cierra la falla y devuelve el equipo a
   * OPERATIVO dentro de la misma transacción, así que la respuesta del POST solo
   * trae el mantenimiento. Sin esto, la falla seguiría abierta en pantalla hasta
   * que alguien pulsara "Actualizar", y el técnico vería un trabajo ya hecho
   * como si estuviera pendiente.
   */
  reflejarFallaResuelta(idFalla: number, idEquipo: number): void {
    this._fallas.update((lista) =>
      lista.map((falla) => (falla.id === idFalla ? { ...falla, estado: 'RESUELTO' } : falla)),
    );

    this._equipos.update((lista) =>
      lista.map((equipo) =>
        equipo.id === idEquipo ? { ...equipo, estado: 'OPERATIVO' as EstadoEquipo } : equipo,
      ),
    );
  }

  /**
   * Trae las fallas y los mantenimientos de un equipo.
   *
   * No se guarda en un signal: es una consulta puntual que pinta una ficha, y
   * cada equipo tiene la suya, así que mezclarla con la lista global haría que
   * al abrir otra ficha se mostraran los datos del equipo anterior.
   */
  obtenerHistorial(idEquipo: number): Observable<HistorialEquipo> {
    return this.http.get<HistorialEquipo>(`${API_EQUIPOS}/${idEquipo}/historial`);
  }

  crearSala(datos: NuevaSala): Observable<Sala> {
    return this.http.post<Sala>(API_SALAS, datos).pipe(
      tap((sala) => this._salas.update((lista) => [...lista, sala])),
    );
  }

  crearEquipo(datos: NuevoEquipo, idSala: number): Observable<Equipo> {
    const params = new HttpParams().set('idSala', idSala);
    return this.http.post<Equipo>(API_EQUIPOS, datos, { params }).pipe(
      tap((equipo) => this._equipos.update((lista) => [...lista, equipo])),
    );
  }

  reportarFalla(datos: NuevaFalla, idEquipo: number): Observable<Falla> {
    const params = new HttpParams().set('idEquipo', idEquipo);
    return this.http.post<Falla>(API_FALLAS, datos, { params }).pipe(
      tap((falla) => this._fallas.update((lista) => [falla, ...lista])),
    );
  }

  registrarMantenimiento(
    datos: NuevoMantenimiento,
    idEquipo: number,
    idFalla: number,
  ): Observable<Mantenimiento> {
    const params = new HttpParams().set('idEquipo', idEquipo).set('idFalla', idFalla);
    return this.http.post<Mantenimiento>(API_MANTENIMIENTOS, datos, { params }).pipe(
      tap((mantenimiento) => this._mantenimientos.update((lista) => [mantenimiento, ...lista])),
    );
  }

  /** Elimina un aula. */
  eliminarSala(id: number): Observable<any> {
    return this.http.delete(`${API_SALAS}/${id}`).pipe(
      tap(() => this._salas.update((lista) => lista.filter((sala) => sala.id !== id))),
    );
  }

  /** Elimina un equipo. */
  eliminarEquipo(id: number): Observable<any> {
    return this.http.delete(`${API_EQUIPOS}/${id}`).pipe(
      tap(() => this._equipos.update((lista) => lista.filter((equipo) => equipo.id !== id))),
    );
  }

  /* --------------------------- Cambios de estado --------------------------- */

  /** Pasa una falla a RESUELTO, EN_REVISION o PENDIENTE. */
  cambiarEstadoFalla(idFalla: number, estado: EstadoFalla): Observable<Falla> {
    const params = new HttpParams().set('estado', estado);
    return this.http
      .patch<Falla>(`${API_FALLAS}/${idFalla}/estado`, null, { params })
      .pipe(
        tap((actualizada) =>
          this._fallas.update((lista) =>
            lista.map((falla) => (falla.id === actualizada.id ? actualizada : falla)),
          ),
        ),
      );
  }

  /** Pone un equipo en OPERATIVO, EN_MANTENIMIENTO o FUERA_DE_SERVICIO. */
  cambiarEstadoEquipo(idEquipo: number, estado: EstadoEquipo): Observable<Equipo> {
    const params = new HttpParams().set('estado', estado);
    return this.http
      .patch<Equipo>(`${API_EQUIPOS}/${idEquipo}/estado`, null, { params })
      .pipe(
        tap((actualizado) =>
          this._equipos.update((lista) =>
            lista.map((equipo) => (equipo.id === actualizado.id ? actualizado : equipo)),
          ),
        ),
      );
  }

  /* ----------------------------- Utilidades ----------------------------- */

  limpiarError(): void {
    this._error.set(null);
  }

  /**
   * Traduce el error del backend a un mensaje entendible.
   *
   * Los controllers devuelven `badRequest().body(e.getMessage())`, así que el
   * cuerpo llega como texto plano; los errores de validación de Spring en
   * cambio vienen en JSON.
   */
  leerError = (error: unknown): string => {
    if (error instanceof HttpErrorResponse) {
      const cuerpo = error.error;

      if (typeof cuerpo === 'string' && cuerpo.trim()) return cuerpo;

      if (cuerpo && typeof cuerpo === 'object') {
        const cuerpoObjeto = cuerpo as Record<string, unknown>;
        const mensaje = cuerpoObjeto['message'] ?? cuerpoObjeto['error'];
        if (typeof mensaje === 'string') return mensaje;
      }

      if (error.status === 0) return 'No hay conexión con el backend. Revisa tu internet o que el servicio esté en marcha.';
      if (error.status === 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      if (error.status === 403) return 'Tu rol no tiene permiso para esta acción.';
      return `El backend respondió con el estado ${error.status}.`;
    }

    return 'Ocurrió un error inesperado.';
  };
}
