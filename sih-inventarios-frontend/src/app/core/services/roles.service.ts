import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { PLATFORM_ID, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { API_USUARIOS } from '../config/api';

/** Rol del catálogo, tal como lo devuelve el backend. */
export interface RolCatalogo {
  id: number;
  nombre: string;
}

/**
 * Catálogo de roles del formulario de registro.
 *
 * La petición se dispara al construirse el servicio (o sea, al cargar la
 * aplicación) en vez de esperar a que el usuario abra el modal. Así, para
 * cuando hace clic, la lista ya está en memoria y el desplegable aparece
 * listo al instante.
 */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly rolesSignal = signal<RolCatalogo[]>([]);
  private readonly cargandoSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly roles = this.rolesSignal.asReadonly();
  readonly cargando = this.cargandoSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor() {
    // Durante el prerender no hay navegador ni peticiones: se espera al cliente.
    if (isPlatformBrowser(this.platformId)) {
      this.cargar();
    }
  }

  /** Descarga el catálogo. No hace nada si ya está cargado o en curso. */
  cargar(): void {
    if (this.rolesSignal().length > 0 || this.cargandoSignal()) {
      return;
    }

    this.cargandoSignal.set(true);
    this.errorSignal.set(null);

    this.http
      .get<RolCatalogo[]>(`${API_USUARIOS}/roles`)
      .pipe(finalize(() => this.cargandoSignal.set(false)))
      .subscribe({
        next: (roles) => this.rolesSignal.set(roles),
        error: (error: HttpErrorResponse) => {
          this.errorSignal.set(
            error.status === 0
              ? 'No hay conexión con el servidor.'
              : 'No se pudieron cargar los roles.',
          );
        },
      });
  }
}
