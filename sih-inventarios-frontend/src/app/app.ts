import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Navbar } from './core/layout/navbar/navbar';

/**
 * Componente raíz. Solo compone el marco: la barra superior y el hueco donde
 * el router dibuja la página activa (portada o dashboard).
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
