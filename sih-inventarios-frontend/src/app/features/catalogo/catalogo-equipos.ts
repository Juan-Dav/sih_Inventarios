import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InventarioService } from '../../core/services/inventario.service';
import { Equipo } from '../../core/models/inventario.model';

@Component({
  selector: 'app-catalogo-equipos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-equipos.html',
  styleUrl: './catalogo-equipos.css',
})
export class CatalogoEquipos implements OnInit {
  equipos = signal<Equipo[]>([]);
  busqueda = signal('');
  cargando = signal(true);
  error = signal<string | null>(null);

  constructor(private inventarioService: InventarioService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.inventarioService.obtenerEquipos().subscribe({
      next: (data) => {
        this.equipos.set(data || []);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al cargar catálogo');
        this.cargando.set(false);
      },
    });
  }

  equiposFiltrados(): Equipo[] {
    const q = this.busqueda().toLowerCase().trim();
    if (!q) return this.equipos();
    return this.equipos().filter((e) => {
      return (
        (e.codigo || '').toLowerCase().includes(q) ||
        ((e as any).nombreEquipo || '').toLowerCase().includes(q) ||
        (e.marca || '').toLowerCase().includes(q) ||
        (e.modelo || '').toLowerCase().includes(q)
      );
    });
  }
}
