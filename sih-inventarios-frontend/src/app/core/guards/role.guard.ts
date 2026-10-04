import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RUTA_POR_ROL, Rol } from '../models/sesion.model';
import { AuthService } from '../services/auth.service';

/**
 * Crea un guard que solo deja pasar a los roles indicados.
 *
 * Si el usuario no tiene permiso no se le muestra un error: se le manda a su
 * propio dashboard, para que escribir la URL de otro rol no lo deje atrapado.
 */
export function roleGuard(rolesPermitidos: readonly Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const usuario = auth.usuario();
    if (!usuario) {
      return router.createUrlTree(['/']);
    }

    if (rolesPermitidos.includes(usuario.rol)) {
      return true;
    }

    return router.createUrlTree([RUTA_POR_ROL[usuario.rol]]);
  };
}
