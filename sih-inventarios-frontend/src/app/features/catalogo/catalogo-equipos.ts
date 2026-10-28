import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-catalogo-equipos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-equipos.html',
  styleUrl: './catalogo-equipos.css',
})
export class CatalogoEquipos implements OnInit {
  equipos = signal<any[]>([]);
  busqueda = signal('');
  cargando = signal(true);
  error = signal<string | null>(null);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.http.get<any[]>(`${environment.apiBaseUrl}/equipos`).subscribe({
      next: (data: any[]) => {
        this.equipos.set(data || []);
        this.cargando.set(false);
      },
      error: (err: any) => {
        this.error.set(err?.error?.message || 'Error al cargar catálogo');
        this.cargando.set(false);
      },
    });
  }

  equiposFiltrados(): any[] {
    const q = this.busqueda().toLowerCase().trim();
    if (!q) return this.equipos();
    return this.equipos().filter((e: any) => {
      return (
        (e.codigo || '').toLowerCase().includes(q) ||
        (e.nombreEquipo || '').toLowerCase().includes(q) ||
        (e.marca || '').toLowerCase().includes(q) ||
        (e.modelo || '').toLowerCase().includes(q)
      );
    });
  }
}
