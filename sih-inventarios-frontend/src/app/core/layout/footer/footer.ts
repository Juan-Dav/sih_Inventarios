import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Footer {
  /** Año de creación del proyecto. */
  protected readonly year = 2026;

  protected readonly links = [
    { label: 'Inicio', href: '#inicio' },
    { label: 'Quiénes somos', href: '#quienes-somos' },
    { label: 'Misión y visión', href: '#mision-vision' },
    { label: 'Objetivos', href: '#objetivos' },
    { label: 'Equipo', href: '#equipo' },
  ] as const;
}
