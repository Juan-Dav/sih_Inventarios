import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Impide entrar a las rutas privadas sin sesión iniciada.
 *
 * Ojo: esto es una barrera de interfaz, no de seguridad. La protección real
 * la aplica el backend, que rechaza con 401 cualquier petición sin JWT válido.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.autenticado() ? true : router.createUrlTree(['/']);
};
