import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';

import { API_USUARIOS } from '../../config/api';
import { ETIQUETA_ROL, ROLES_REGISTRO, Rol, UsuarioSesion } from '../../models/sesion.model';
import { AuthService } from '../../services/auth.service';
import { Dialogo, DialogoTipo } from '../../ui/dialogo/dialogo';

interface NavLink {
  label: string;
  fragment: string;
}

/** Estado del diálogo de resultado que se muestra encima de la página. */
interface DialogoEstado {
  tipo: DialogoTipo;
  titulo: string;
  mensaje: string;
  textoAccion: string;
  /** Datos del saludo, solo para el diálogo de bienvenida. */
  usuario?: UsuarioSesion;
}

@Component({
  selector: 'app-navbar',
  imports: [FormsModule, Dialogo],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Navbar {
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);
private readonly auth = inject(AuthService);
private readonly router = inject(Router);

  protected readonly apiUrl = API_USUARIOS;

  /** Sesión activa, para cambiar los botones de la barra. */
protected readonly usuario = this.auth.usuario;
protected readonly etiquetaRol = ETIQUETA_ROL;

/** Opciones del desplegable de rol del formulario de alta. */
protected readonly rolesRegistro = ROLES_REGISTRO;

  protected readonly scrolled = signal(false);
  protected readonly menuOpen = signal(false);

  /**
   * URL en la que estamos, reactiva.
   *
   * Se necesita porque dentro de los paneles la barra pública estorba: los
   * enlaces "Quiénes somos", "Misión y visión" y compañía no llevan a ningún
   * sitio, el panel ya trae su propio menú lateral. En la portada sí se
   * muestran y cumplen su función normal.
   */
  private readonly urlActual = toSignal(
    this.router.events.pipe(
      filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly enPanel = computed(() => this.urlActual().startsWith('/dashboard'));

  /**
   * La barra necesita fondo oscuro siempre, no solo al hacer scroll.
   *
   * En la portada el fondo de la página es oscuro, así que sin scroll puede
   * quedar transparente y todo sigue viéndose. Dentro de un panel no: los
   * módulos son de estilo claro y el texto de la barra es blanco, así que
   * transparente lo dejaba ilegible. Por eso dentro del panel el fondo se
   * aplica siempre.
   */
  protected readonly barraSolida = computed(() => this.scrolled() || this.enPanel());

  /** Dentro del panel la barra va siempre en alto compacto, para no robar espacio. */
  protected readonly barraCompacta = computed(() => this.scrolled() || this.enPanel());

  protected readonly isLoginModalOpen = signal(false);
  protected readonly isRegisterModalOpen = signal(false);

  /** Evita el doble envío mientras la petición está en vuelo. */
  protected readonly procesando = signal(false);

  /**
   * `null` = el scroll del body está libre. Cadena = valor previo que hay que
   * restaurar al cerrar el modal.
   */
  protected readonly bodyOverflow = signal<string | null>(null);

  /** Diálogo de resultado (alta, error o bienvenida). */
  protected readonly dialogo = signal<DialogoEstado | null>(null);

  /** Iniciales para el avatar del saludo. */
  protected readonly iniciales = computed(() => {
    const nombre = this.dialogo()?.usuario?.nombre?.trim() ?? '';
    if (!nombre) return '';

    return nombre
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase() ?? '')
      .join('');
  });

  protected readonly links: readonly NavLink[] = [
    { label: 'Inicio', fragment: 'inicio' },
    { label: 'Quiénes somos', fragment: 'quienes-somos' },
    { label: 'Misión y visión', fragment: 'mision-vision' },
    { label: 'Objetivos', fragment: 'objetivos' },
    { label: 'Equipo', fragment: 'equipo' },
  ];

  protected loginData = {
    correo: '',
    contrasena: '',
  };

  /**
   * Datos del formulario de alta.
   *
   * `rol` va tipado como `Rol` para poder indexar `etiquetaRol` directamente.
   * El `as Rol` es necesario porque si no el objeto se widenaría a `string` y
   * ninguna variante del tipo encajaría.
   */
  protected registerData = {
    nombre: '',
    correo: '',
    contrasena: '',
    rol: 'DOCENTE' as Rol,
  };

  constructor() {
    // Si se entra a un panel con el menú móvil abierto, se cierra: sus enlaces
    // son de la portada y allí no llevan a ninguna parte.
    effect(() => {
      if (this.enPanel()) this.menuOpen.set(false);
    });

    afterNextRender(() => {
      // Se agrupa con requestAnimationFrame: el evento `scroll` se dispara
      // muchas más veces que los fotogramas y así el trabajo real queda
      // como máximo a 60 veces por segundo.
      let queued = false;

      const update = () => {
        queued = false;
        this.scrolled.set(window.scrollY > 24);
      };

      const onScroll = () => {
        if (queued) return;
        queued = true;
        requestAnimationFrame(update);
      };

      update();
      window.addEventListener('scroll', onScroll, { passive: true });
      this.destroyRef.onDestroy(() => window.removeEventListener('scroll', onScroll));
    });
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  /**
   * Navega a la portada y baja hasta la sección elegida.
   *
   * Se usa el fragment del router en vez de un href="#ancla" pelado: así
   * funciona igual estando ya en la portada o llegando desde un dashboard.
   */
  protected irA(fragment: string): void {
    this.closeMenu();
    this.router.navigate(['/'], { fragment });
  }

  /* ---------------------------------------------------------------
     Modales
     --------------------------------------------------------------- */

  protected abrirLogin(): void {
    this.isRegisterModalOpen.set(false);
    this.isLoginModalOpen.set(true);
    this.lockScroll();
  }

  protected cerrarLogin(): void {
    this.isLoginModalOpen.set(false);
    this.unlockScroll();
  }

  protected abrirRegistro(): void {
    this.isLoginModalOpen.set(false);
    this.isRegisterModalOpen.set(true);
    this.lockScroll();
  }

  protected cerrarRegistro(): void {
    this.isRegisterModalOpen.set(false);
    this.unlockScroll();
  }

  /** Cierra el modal abierto pulsando el fondo oscuro. */
  protected cerrarModalActivo(): void {
    this.cerrarLogin();
    this.cerrarRegistro();
  }

  /**
   * Guarda el `overflow` actual del body para bloquear el scroll de fondo
   * sin perder el valor previo al cerrar el modal.
   *
   * El centinela es `null` y no `''` a propósito: el valor previo del body
   * suele ser la cadena vacía, y `''` es falsy, así que con `''` como marca la
   * página se quedaba con `overflow: hidden` para siempre después del primer
   * modal. Con `null` se distingue "no hay nada bloqueado" de "el valor
   * anterior era vacío", que es justo el caso normal.
   */
  private lockScroll(): void {
    if (this.bodyOverflow() !== null) return;
    this.bodyOverflow.set(document.body.style.overflow);
    document.body.style.overflow = 'hidden';
  }

  private unlockScroll(): void {
    const previous = this.bodyOverflow();
    if (previous === null) return;
    document.body.style.overflow = previous;
    this.bodyOverflow.set(null);
  }

  /* ---------------------------------------------------------------
     Diálogo de resultado
     --------------------------------------------------------------- */

  protected cerrarDialogo(): void {
    this.dialogo.set(null);
  }

  /**
   * Al cerrar el diálogo de alta se ofrece entrar con la cuenta recién
   * creada, que ya quedó activa.
   */
  protected irALogin(): void {
    this.cerrarDialogo();
    this.abrirLogin();
  }

  /* ---------------------------------------------------------------
     Validación
     --------------------------------------------------------------- */

  /**
   * Campos vacíos del formulario de entrada.
   *
   * Solo se revisan correo y contraseña, que son los únicos campos que tiene.
   * El formato del correo y la longitud mínima los avisa el backend con un
   * mensaje más preciso que "falta este campo".
   */
  private faltantesLogin(): string[] {
    const faltantes: string[] = [];

    if (!this.loginData.correo.trim()) faltantes.push('Correo');
    if (!this.loginData.contrasena.trim()) faltantes.push('Contraseña');

    return faltantes;
  }

  /**
   * Campos vacíos del formulario de alta.
   *
   * Va campo por campo a propósito: una lista genérica exige los obligatorios
   * del formulario equivocado, que es como el login llegó a reclamar "Nombre"
   * y "Rol" a un usuario que solo tiene correo y contraseña.
   */
  private faltantesRegistro(): string[] {
    const faltantes: string[] = [];

    if (!this.registerData.nombre.trim()) faltantes.push('Nombre completo');
    if (!this.registerData.correo.trim()) faltantes.push('Correo');
    if (!this.registerData.contrasena.trim()) faltantes.push('Contraseña');
    if (!this.registerData.rol.trim()) faltantes.push('Rol');

    return faltantes;
  }

  private mostrarErrorCampos(faltantes: string[]): void {
    this.dialogo.set({
      tipo: 'error',
      titulo: 'Faltan datos por completar',
      mensaje:
        faltantes.length > 1
          ? `Completa estos campos para continuar: ${faltantes.join(', ')}.`
          : `El campo "${faltantes[0]}" es obligatorio.`,
      textoAccion: 'Completar',
    });
  }

  /* ---------------------------------------------------------------
     Peticiones al backend
     --------------------------------------------------------------- */

  /**
   * Inicia sesión, muestra el saludo con nombre y rol, y al aceptarlo
   * lleva al dashboard que le corresponde.
   */
  protected procesarLogin(form: NgForm): void {
    const faltantes = this.faltantesLogin();
    if (faltantes.length > 0) {
      this.mostrarErrorCampos(faltantes);
      return;
    }

    this.procesando.set(true);

    const { correo, contrasena } = this.loginData;

    this.auth.login(correo, contrasena).subscribe({
      next: (usuario) => {
        this.procesando.set(false);
        this.cerrarLogin();
        this.loginData = { correo: '', contrasena: '' };

        this.dialogo.set({
          tipo: 'info',
          titulo: `¡Bienvenido, ${usuario.nombre.split(' ')[0]}!`,
          mensaje: `Iniciaste sesión con el rol de ${this.etiquetaRol[usuario.rol]}.`,
          textoAccion: 'Ir a mi panel',
          usuario,
        });
      },
      error: (error: HttpErrorResponse) => {
        this.procesando.set(false);
        this.dialogo.set({
          tipo: 'error',
          titulo: 'No pudimos iniciar sesión',
          mensaje: this.extractMessage(error, 'Revisa el backend y vuelve a intentar.'),
          textoAccion: 'Reintentar',
        });
      },
    });
  }

  /** Al aceptar el saludo, entra al dashboard según el rol del token. */
  protected irAlDashboard(): void {
    this.cerrarDialogo();
    this.router.navigateByUrl(this.auth.dashboardActual() ?? '/');
  }

  /**
   * Cierra la sesión y vuelve a la portada.
   *
   * Antes de salir se cierra cualquier modal: si se cerrara la sesión con
   * uno abierto, el `overflow: hidden` que puso `lockScroll` se quedaría puesto
   * en la portada y el usuario llegaría a una página que no se puede desplazar.
   */
  protected cerrarSesion(): void {
    this.cerrarModalActivo();
    this.cerrarDialogo();
    this.auth.logout();
  }

  /**
   * POST {api}/registro?rol=<elegido>
   *
   * El rol viaja en la query string y el body solo lleva los datos del usuario.
   * Lo elige quien se registra; el backend acepta los tres roles y deja la cuenta
   * activa, así que el administrador puede corregirlo después desde su panel si
   * alguien se apuntara con un rol que no le corresponde.
   */
  protected procesarRegistro(form: NgForm): void {
    const faltantes = this.faltantesRegistro();
    if (faltantes.length > 0) {
      this.mostrarErrorCampos(faltantes);
      return;
    }

    this.procesando.set(true);

    const { nombre, correo, contrasena, rol } = this.registerData;
    const url = `${this.apiUrl}/registro?rol=${encodeURIComponent(rol)}`;

    this.http.post(url, { nombre, correo, contrasena }).subscribe({
      next: () => {
        this.procesando.set(false);
        this.cerrarRegistro();
        this.registerData = {
          nombre: '',
          correo: '',
          contrasena: '',
          rol: 'DOCENTE',
        };

        this.dialogo.set({
          tipo: 'exito',
          titulo: '¡Cuenta creada con éxito!',
          mensaje:
            `Tu cuenta de ${nombre.trim()} ya está activa con rol ${this.etiquetaRol[rol]}. ` +
            'Ya puedes iniciar sesión.',
          textoAccion: 'Iniciar sesión',
        });
      },
      error: (error: HttpErrorResponse) => {
        this.procesando.set(false);
        this.dialogo.set({
          tipo: 'error',
          titulo: 'No se pudo crear la cuenta',
          mensaje: this.extractMessage(error, 'Inténtalo de nuevo.'),
          textoAccion: 'Cerrar',
        });
      },
    });
  }

  /**
   * Spring Boot suele devolver { message: "..." } o { error: "..." } o un
   * texto plano cuando el controller hace badRequest().body(e.getMessage()).
   */
  private extractMessage(source: unknown, fallback: string): string {
    if (typeof source === 'string') return source;
    if (source && typeof source === 'object') {
      const body = source as Record<string, unknown>;
      for (const key of ['message', 'error', 'mensaje', 'detail']) {
        const value = body[key];
        if (typeof value === 'string') return value;
      }
    }
    return fallback;
  }
}
