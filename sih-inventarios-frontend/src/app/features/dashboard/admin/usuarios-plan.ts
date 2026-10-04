import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ESTILO_ESTADO_USUARIO, Usuario } from '../../../core/models/plan.model';
import { PlanService } from '../../../core/services/plan.service';
import { RolesService } from '../../../core/services/roles.service';
import { InventarioService } from '../../../core/services/inventario.service';
import { Planes } from '../shared/planes';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Módulo de cuentas y plan del administrador.
 *
 * Son las dos funciones sin equivalente en los otros paneles: aquí se decide
 * quién entra al sistema y con qué rol, y hasta dónde llega la demostración
 * gratuita.
 */
@Component({
  selector: 'app-usuarios-plan',
  imports: [FormsModule, Planes],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Plan ============ -->
<!-- Se reutiliza el módulo que tienen el técnico y el docente en vez de
           repetir aquí las tarjetas: los tres paneles contratan y cancelan el
           mismo plan de la institución, y duplicar el formulario hacía que cada
           arreglo en uno dejara a los otros dos desactualizados. -->
      <app-planes />

      <!-- ============ Alta / edición y listado ============ -->
      <div class="grid gap-6 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)]">
        <section class="tarjeta">
          <h2 class="titulo-modulo">{{ editando() ? 'Editar cuenta' : 'Crear cuenta' }}</h2>
          <p class="subtitulo-modulo">
            {{
              editando()
                ? 'Corrige los datos de la cuenta seleccionada.'
                : 'Registra a alguien con el rol que le corresponda.'
            }}
          </p>

          <form class="mt-5 flex flex-col gap-4" (ngSubmit)="guardar()">
            <div>
              <label class="rotulo" for="up-nombre">Nombre completo</label>
              <input
                id="up-nombre"
                name="nombre"
                type="text"
                class="campo"
                maxlength="100"
                [(ngModel)]="form.nombre"
                [disabled]="operando()"
              />
            </div>

            <div>
              <label class="rotulo" for="up-correo">Correo</label>
              <input
                id="up-correo"
                name="correo"
                type="email"
                class="campo"
                maxlength="100"
                [(ngModel)]="form.correo"
                [disabled]="operando()"
              />
            </div>

            @if (!editando()) {
              <div>
                <label class="rotulo" for="up-contrasena">Contraseña</label>
                <input
                  id="up-contrasena"
                  name="contrasena"
                  type="text"
                  class="campo"
                  minlength="6"
                  [(ngModel)]="form.contrasena"
                  [disabled]="operando()"
                />
              </div>

              <div>
                <label class="rotulo" for="up-rol">Rol</label>
                <select
                  id="up-rol"
                  name="rol"
                  class="campo"
                  [(ngModel)]="form.rol"
                  [disabled]="operando()"
                >
                  @for (rol of roles(); track rol.id) {
                    <option [ngValue]="rol.nombre">{{ rol.nombre }}</option>
                  }
                </select>
              </div>
            }

            <button
              type="submit"
              class="botón bg-gradient-to-r from-brand-600 to-pop-600 text-white shadow-lg shadow-brand-600/25 hover:shadow-brand-600/40"
              [disabled]="operando()"
            >
              {{ operando() ? 'Guardando…' : editando() ? 'Guardar cambios' : 'Crear cuenta' }}
            </button>

            @if (editando()) {
              <button
                type="button"
                class="botón boton-secundario"
                [disabled]="operando()"
                (click)="cancelarEdicion()"
              >
                Cancelar
              </button>
            }
          </form>
        </section>

        <section class="tarjeta">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="titulo-modulo">Cuentas del sistema</h2>
              <p class="subtitulo-modulo">Cambia el rol, corrige los datos o desactiva el acceso.</p>
            </div>

            <button
              type="button"
              class="botón boton-acento"
              [disabled]="planService.cargandoUsuarios()"
              (click)="planService.cargarUsuarios()"
            >
              {{ planService.cargandoUsuarios() ? 'Actualizando…' : 'Actualizar' }}
            </button>
          </div>

          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th class="w-44">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (usuario of planService.usuarios(); track usuario.id) {
                  <tr>
                    <td>
                      <span class="font-bold text-ink-900">{{ usuario.nombre }}</span>
                      <span class="block text-xs text-ink-500">{{ usuario.correo }}</span>
                    </td>
                    <td>
                      <select
                        class="campo !w-auto !py-1 !text-xs"
                        [value]="usuario.rol.nombre"
                        [disabled]="operando()"
                        (change)="cambiarRol(usuario, $event)"
                        [attr.aria-label]="'Cambiar rol de ' + usuario.nombre"
                      >
                        @for (rol of roles(); track rol.id) {
                          <option [value]="rol.nombre">{{ rol.nombre }}</option>
                        }
                      </select>
                    </td>
                    <td>
                      <span class="etiqueta-estado" [class]="estiloEstado(usuario.estado).clases">
                        {{ estiloEstado(usuario.estado).texto }}
                      </span>
                    </td>
                    <td>
                      <div class="flex items-center gap-2">
                        <button
                          type="button"
                          class="botón boton-secundario boton-fila"
                          [disabled]="operando()"
                          (click)="editar(usuario)"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          class="botón boton-fila"
                          [class]="estaActivo(usuario) ? et.desactivar : et.activar"
                          [disabled]="operando()"
                          (click)="alternarEstado(usuario)"
                        >
                          {{ estaActivo(usuario) ? 'Desactivar' : 'Activar' }}
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4">
                      <div class="vacio">
                        <h3>Todavía no hay cuentas</h3>
                        <p>Crea la primera con el formulario de al lado.</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      </div>

      @if (aviso(); as mensaje) {
        <p
          class="aviso"
          [class.bg-mint-400/15]="mensaje.tipo === 'exito'"
          [class.txt-ok]="mensaje.tipo === 'exito'"
          [class.bg-pop-500/15]="mensaje.tipo === 'error'"
          [class.txt-alerta]="mensaje.tipo === 'error'"
        >
          {{ mensaje.texto }}
        </p>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsuariosPlan implements OnInit {
  protected readonly planService = inject(PlanService);

  private readonly rolesService = inject(RolesService);

  /** Se reutiliza el traductor de errores del inventario: el backend responde igual en toda la API. */
  private readonly inventario = inject(InventarioService);

  protected readonly roles = this.rolesService.roles;

  /** Clases de estado repetidas en la plantilla, agrupadas para no repetirlas en cada uso. */
  protected readonly et = {
    desactivar: 'border border-pop-400/40 bg-pop-500/15 text-[#f9a8d4] hover:bg-pop-500/25',
    activar: 'border border-mint-400/40 bg-mint-400/15 text-[#6ee7b7] hover:bg-mint-400/25',
  } as const;

  protected readonly aviso = signal<Aviso | null>(null);
  protected readonly operando = signal(false);

  /** Cuenta abierta en el formulario, o `null` si el formulario está creando una nueva. */
  protected readonly editando = signal<Usuario | null>(null);

  protected form = { nombre: '', correo: '', contrasena: '', rol: 'DOCENTE' };

  ngOnInit(): void {
    this.planService.cargarUsuarios();
    this.rolesService.cargar();
  }

  protected estiloEstado(estado: string): { texto: string; clases: string } {
    return (
      ESTILO_ESTADO_USUARIO[estado] ?? {
        texto: estado || 'Sin estado',
        clases: 'bg-ink-100 text-ink-500 ring-1 ring-inset ring-ink-200',
      }
    );
  }

  protected estaActivo(usuario: Usuario): boolean {
    return usuario.estado === 'ACTIVO';
  }

  /* ---------------------------- Usuarios ---------------------------- */

  protected guardar(): void {
    this.aviso.set(null);

    const nombre = this.form.nombre.trim();
    const correo = this.form.correo.trim();

    if (!nombre) {
      this.aviso.set({ tipo: 'error', texto: 'Escribe el nombre completo.' });
      return;
    }
    if (!correo) {
      this.aviso.set({ tipo: 'error', texto: 'Escribe el correo.' });
      return;
    }

    const enEdicion = this.editando();

    if (!enEdicion && this.form.contrasena.trim().length < 6) {
      this.aviso.set({ tipo: 'error', texto: 'La contraseña debe tener al menos 6 caracteres.' });
      return;
    }

    this.operando.set(true);

    if (enEdicion) {
      this.planService.actualizarUsuario(enEdicion.id, { nombre, correo }).subscribe({
        next: () => this.finOperacion('Cuenta actualizada.'),
        error: (error: unknown) => this.fallar(error),
      });
      return;
    }

    this.planService
      .crearUsuario(
        { nombre, correo, contrasena: this.form.contrasena.trim() },
        this.form.rol,
      )
      .subscribe({
        next: (creado) => {
          // El alta reescribe el formulario, así que el aviso se compone aquí y
          // no en `finOperacion`, que además cancelaría la edición.
          this.form = { nombre: '', correo: '', contrasena: '', rol: 'DOCENTE' };
          this.operando.set(false);
          this.aviso.set({ tipo: 'exito', texto: `Cuenta creada para ${creado.nombre}.` });
        },
        error: (error: unknown) => this.fallar(error),
      });
  }

  protected editar(usuario: Usuario): void {
    this.aviso.set(null);
    this.editando.set(usuario);
    this.form = {
      nombre: usuario.nombre,
      correo: usuario.correo,
      contrasena: '',
      rol: usuario.rol.nombre,
    };
  }

  protected cancelarEdicion(): void {
    this.editando.set(null);
    this.form = { nombre: '', correo: '', contrasena: '', rol: 'DOCENTE' };
  }

  protected cambiarRol(usuario: Usuario, evento: Event): void {
    const rol = (evento.target as HTMLSelectElement).value;
    if (!rol || rol === usuario.rol.nombre) return;

    this.aviso.set(null);
    this.operando.set(true);

    this.planService.asignarRol(usuario.id, rol).subscribe({
      next: () => this.finOperacion(`${usuario.nombre} ahora es ${rol}.`),
      error: (error: unknown) => {
        this.fallar(error);
        // El desplegable se repinta desde la lista, que sigue con el rol viejo:
        // si solo se cambiara el signal de error, el <select> mantendría el valor
        // que eligió el usuario aunque el backend lo haya rechazado.
        this.planService.cargarUsuarios();
      },
    });
  }

  protected alternarEstado(usuario: Usuario): void {
    const siguiente = this.estaActivo(usuario) ? 'INACTIVO' : 'ACTIVO';

    this.aviso.set(null);
    this.operando.set(true);

    this.planService.cambiarEstado(usuario.id, siguiente).subscribe({
      next: () =>
        this.finOperacion(
          `${usuario.nombre} quedó ${siguiente === 'ACTIVO' ? 'activo' : 'desactivado'}.`,
        ),
      error: (error: unknown) => this.fallar(error),
    });
  }

  private finOperacion(mensaje: string): void {
    this.operando.set(false);
    this.cancelarEdicion();
    this.aviso.set({ tipo: 'exito', texto: mensaje });
  }

  private fallar(error: unknown): void {
    this.operando.set(false);
    this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
  }
}
