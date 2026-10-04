import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { PLATFORM_ID, computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';

import { API_USUARIOS } from '../config/api';
import { LoginResponse, RUTA_POR_ROL, Rol, UsuarioSesion } from '../models/sesion.model';

const CLAVE_TOKEN = 'sih.token';
const CLAVE_USUARIO = 'sih.usuario';

/**
 * Estado de sesión del usuario.
 *
 * El token vive en localStorage y la información del usuario se mantiene en
 * signals, de modo que las plantillas reacciona solas.
 *
 * Ojo con el SSR: durante el prerender no existe localStorage, así que la
 * restauración se hace únicamente en el navegador.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(null);
  private readonly usuarioSignal = signal<UsuarioSesion | null>(null);

  readonly token = this.tokenSignal.asReadonly();
  readonly usuario = this.usuarioSignal.asReadonly();
  readonly autenticado = computed(() => this.usuarioSignal() !== null);
  readonly rol = computed<Rol | null>(() => this.usuarioSignal()?.rol ?? null);

  /** A dónde debe ir este usuario según su rol. */
  readonly dashboardActual = computed<string | null>(() => {
    const rol = this.rol();
    return rol ? RUTA_POR_ROL[rol] : null;
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.restaurarSesion();
    }
  }

  /** Autentica contra el backend y guarda el token. */
  login(correo: string, contrasena: string): Observable<UsuarioSesion> {
    return this.http
      .post<LoginResponse>(`${API_USUARIOS}/login`, { correo, contrasena })
      .pipe(map((respuesta) => this.persistir(respuesta)));
  }

  /** Cierra sesión y vuelve a la página de inicio. */
  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(CLAVE_TOKEN);
      localStorage.removeItem(CLAVE_USUARIO);
    }

    this.tokenSignal.set(null);
    this.usuarioSignal.set(null);
    this.router.navigate(['/']);
  }

  /** Igual que logout pero sin navegar: lo usa el interceptor ante un 401. */
  limpiarSesion(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(CLAVE_TOKEN);
      localStorage.removeItem(CLAVE_USUARIO);
    }

    this.tokenSignal.set(null);
    this.usuarioSignal.set(null);
  }

  /**
   * Recupera la sesión al recargar la página.
   *
   * Se reconstruye leyendo el propio token en vez de llamar a /me, así la
   * primera navegación tras la recarga ya sabe el rol y los guards no
   * expulsan al usuario a la portada.
   */
  private restaurarSesion(): void {
    const token = localStorage.getItem(CLAVE_TOKEN);
    if (!token) return;

    const payload = this.decodificar(token);
    if (!payload) {
      this.limpiarSesion();
      return;
    }

    // "exp" viene en segundos desde epoch; el backend lo firma en segundos.
    const exp = payload['exp'];
    if (typeof exp === 'number' && exp * 1000 <= Date.now()) {
      this.limpiarSesion();
      return;
    }

    const guardado = localStorage.getItem(CLAVE_USUARIO);
    if (!guardado) {
      this.limpiarSesion();
      return;
    }

    try {
      this.tokenSignal.set(token);
      this.usuarioSignal.set(JSON.parse(guardado) as UsuarioSesion);
    } catch {
      this.limpiarSesion();
    }
  }

  private persistir(respuesta: LoginResponse): UsuarioSesion {
    const usuario: UsuarioSesion = {
      id: respuesta.id,
      nombre: respuesta.nombre,
      correo: respuesta.correo,
      rol: respuesta.rol,
      estado: respuesta.estado,
    };

    localStorage.setItem(CLAVE_TOKEN, respuesta.token);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(usuario));

    this.tokenSignal.set(respuesta.token);
    this.usuarioSignal.set(usuario);

    return usuario;
  }

  /**
   * Lee el payload del JWT sin verificar la firma.
   *
   * No es un riesgo: la firma la comprueba el backend en cada petición. Aquí
   * solo se leen datos para pintar la interfaz.
   */
  private decodificar(token: string): Record<string, unknown> | null {
    try {
      const partes = token.split('.');
      if (partes.length !== 3) return null;

      const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
      const relleno = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');

      return JSON.parse(atob(relleno)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
