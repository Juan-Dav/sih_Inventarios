import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE } from '../../../core/config/api';
import { Equipo, Sala } from '../../../core/models/inventario.model';
import { SalasEquipos } from './salas-equipos';

/**
 * Regresión del QR: antes se abría una pestaña y, si la petición fallaba, se
 * cerraba sola antes de que diera tiempo a escanear el código. Ahora el QR se
 * pinta en un modal que solo se cierra cuando la persona lo decide.
 */
describe('SalasEquipos - modal del QR', () => {
  let httpMock: HttpTestingController;

  const sala: Sala = { id: 1, nombre: 'Aula 101', capacidadMaxima: 12 };
  const equipo: Equipo = {
    id: 1,
    codigo: 'PC-001',
    caracteristicas: 'Intel i5',
    estado: 'OPERATIVO',
    sala,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalasEquipos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('abre el modal y lo deja abierto hasta que la persona lo cierre', async () => {
    const abrirVentana = vi.spyOn(window, 'open');
    const fixture = crearComponente();
    fixture.detectChanges();

    responderCargaInicial();
    fixture.detectChanges();

    pulsarGenerarQr(fixture);

    // El QR ya no vive en una pestaña emergente que el navegador pueda cerrar.
    expect(abrirVentana).not.toHaveBeenCalled();

    // El modal aparece de inmediato, mientras la imagen se está descargando.
    expect(dialogo(fixture)).not.toBeNull();
    expect(dialogo(fixture)?.textContent).toContain('Generando el código QR');

    const peticion = httpMock.expectOne(
      (req) => req.url === `${API_BASE}/equipos/${equipo.id}/qr/imagen`,
    );
    expect(peticion.request.responseType).toBe('blob');
    const enlace = 'https://sih-inventarios.netlify.app/consulta/bfd733d2-64c1-4193-a049-02b9027fca2b';
    peticion.flush(
      new Blob([new Uint8Array([137, 80, 78, 71])], { type: 'image/png' }),
      { headers: { 'X-Qr-Url': enlace } },
    );

    await vi.waitFor(() => {
      fixture.detectChanges();
      const imagen = dialogo(fixture)?.querySelector('img');
      expect(imagen?.src).toContain('data:image/png;base64');
    });

    // Sigue abierto después de cargar: se puede escanear con calma.
    expect(dialogo(fixture)).not.toBeNull();
    expect(dialogo(fixture)?.textContent).toContain('Descargar PNG');

    // La URL real del QR queda a la vista y se puede abrir en otra pestaña.
    const abrir = dialogo(fixture)?.querySelector('a[target="_blank"]') as HTMLAnchorElement | null;
    expect(abrir?.getAttribute('href')).toBe(enlace);
    expect(dialogo(fixture)?.textContent).toContain(enlace);
  });

  it('si el backend falla, muestra el error en el modal en vez de cerrarlo', async () => {
    const fixture = crearComponente();
    fixture.detectChanges();

    responderCargaInicial();
    fixture.detectChanges();

    pulsarGenerarQr(fixture);

    httpMock
      .expectOne((req) => req.url === `${API_BASE}/equipos/${equipo.id}/qr/imagen`)
      .flush(new Blob(['El equipo no existe'], { type: 'text/plain' }), {
        status: 500,
        statusText: 'Server Error',
      });

    fixture.detectChanges();

    expect(dialogo(fixture)).not.toBeNull();
    expect(dialogo(fixture)?.textContent).toContain('El backend respondió con el estado 500');
  });

  function crearComponente() {
    const fixture = TestBed.createComponent(SalasEquipos);
    fixture.componentRef.setInput('gradiente', 'bg-gradient-to-br from-brand-600 to-pop-600');
    fixture.componentRef.setInput('gradienteSolido', 'bg-gradient-to-r from-brand-600 to-pop-600');
    fixture.componentRef.setInput('permiteCrear', true);
    fixture.componentRef.setInput('permiteEditar', true);
    fixture.componentRef.setInput('permiteCambiarEstado', true);

    // PlanService pide el plan en su constructor, antes del primer detectChanges.
    httpMock.expectOne(`${API_BASE}/plan`).flush({
      plan: 'GRATUITO',
      salasRegistradas: 1,
      equiposRegistrados: 1,
      maxSalas: 2,
      salasIlimitadas: false,
      maxEquiposPorSala: 12,
      equiposIlimitados: false,
    });

    return fixture;
  }

  function responderCargaInicial() {
    httpMock.expectOne(`${API_BASE}/salas`).flush([sala]);
    httpMock.expectOne(`${API_BASE}/equipos`).flush([equipo]);
    httpMock.expectOne(`${API_BASE}/fallas`).flush([]);
    httpMock.expectOne(`${API_BASE}/mantenimientos`).flush([]);
  }

  function pulsarGenerarQr(fixture: ReturnType<typeof TestBed.createComponent<SalasEquipos>>) {
    const botones = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    );
    const boton = botones.find((elemento) => elemento.textContent?.includes('Generar QR'));
    expect(boton).toBeTruthy();
    boton?.click();
    fixture.detectChanges();
  }

  function dialogo(fixture: ReturnType<typeof TestBed.createComponent<SalasEquipos>>) {
    return fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement | null;
  }
});
