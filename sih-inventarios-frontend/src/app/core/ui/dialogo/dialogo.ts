import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export type DialogoTipo = 'exito' | 'error' | 'info';

/**
 * Diálogo de resultado reutilizable.
 *
 * Mantiene el diseño del proyecto (vidrio oscuro, degradado de marca) y añade
 * la animación del icono: un chulo que se dibuja al cerrarse el círculo, o
 * una equis que se dibuja con un temblor de "no".
 *
 * El contenido extra se proyecta con <ng-content>, que es lo que usa el
 * saludo de bienvenida para mostrar el avatar y el rol.
 */
@Component({
  selector: 'app-dialogo',
  templateUrl: './dialogo.html',
  styleUrl: './dialogo.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dialogo {
  readonly tipo = input.required<DialogoTipo>();
  readonly titulo = input.required<string>();
  readonly mensaje = input('');
  readonly textoAccion = input('Entendido');

  readonly cerrado = output<void>();
}
