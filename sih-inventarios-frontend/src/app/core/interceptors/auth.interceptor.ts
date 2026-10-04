import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/** Peticiones que se hacen sin token (o cuyo 401 no significa sesión caducada). */
const RUTAS_PUBLICAS = ['/login', '/registro', '/roles'];

/**
 * Añade la cabecera "Authorization: Bearer <token>" a las peticiones
 * protegidas y limpia la sesión cuando el backend responde 401.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();

  const esPublica = RUTAS_PUBLICAS.some((ruta) => request.url.includes(ruta));

  // El login, el registro y el catálogo de roles van sin token.
  if (!token || esPublica) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 en una ruta protegida: el token caducó o el usuario fue borrado.
      if (error.status === 401) {
        auth.limpiarSesion();
        router.navigate(['/'], { queryParams: { sesion: 'expirada' } });
      }

      return throwError(() => error);
    }),
  );
};
