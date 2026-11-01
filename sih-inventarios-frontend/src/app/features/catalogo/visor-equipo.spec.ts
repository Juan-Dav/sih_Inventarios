import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, convertToParamMap } from '@angular/router';

import { API_BASE } from '../../core/config/api';
import { VisorEquipo } from './visor-equipo';

/**
 * La ficha que se abre al escanear el QR.
 *
 * Estos tests fijan lo que el celular debe ver: los datos reales que devuelve
 * el backend, nunca el JSON crudo, y un mensaje claro cuando el código no
 * corresponde a ningún equipo.
 */
describe('VisorEquipo - ficha pública del QR', () => {
  let httpMock: HttpTestingController;
  let parametros: ParamMap;

  beforeEach(async () => {
    parametros = convertToParamMap({ codigo: 'codigo-1' });

    await TestBed.configureTestingModule({
      imports: [VisorEquipo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            get snapshot() {
              return { paramMap: parametros };
            },
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('pinta la información real del equipo que devuelve el backend', () => {
    const fixture = TestBed.createComponent(VisorEquipo);
    fixture.detectChanges();

    const peticion = httpMock.expectOne(`${API_BASE}/consulta/qr/codigo-1`);
    // El HttpClient de Angular pide JSON explícitamente, así que el backend
    // no le responde la tabla HTML.
    expect(peticion.request.responseType).toBe('json');

    peticion.flush({
      id: 5,
      codigo: 'PC-002',
      codigoQr: 'codigo-1',
      nombreEquipo: 'Lenovo ThinkCentre M70',
      categoria: 'Computador',
      caracteristicas: 'Intel i5, 16 GB RAM',
      marca: 'Lenovo',
      modelo: 'ThinkCentre M70',
      numeroSerie: 'SN-123',
      fechaAdquisicion: '2025-01-15',
      garantiaHasta: '2027-01-15',
      ubicacionDetalle: 'Laboratorio 2',
      observaciones: 'Sin novedades',
      activo: true,
      estado: 'OPERATIVO',
      sala: 'Sala 1A',
      area: 'Tecnología',
      departamento: 'Sistemas',
    });

    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('PC-002');
    expect(texto).toContain('Lenovo ThinkCentre M70');
    expect(texto).toContain('Sala 1A');
    expect(texto).toContain('Operativo');
    expect(texto).toContain('SN-123');
    expect(texto).toContain('Intel i5, 16 GB RAM');
    // La ficha de Angular no reutiliza la tabla HTML del backend.
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });

  it('muestra un mensaje claro cuando el código no existe', () => {
    const fixture = TestBed.createComponent(VisorEquipo);
    fixture.detectChanges();

    httpMock
      .expectOne(`${API_BASE}/consulta/qr/codigo-1`)
      .flush(
        { message: 'Código QR no válido o equipo no encontrado' },
        { status: 404, statusText: 'Not Found' },
      );

    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('No pudimos abrir la ficha');
    expect(texto).toContain('No encontramos ningún equipo con este código QR');
    expect(texto).not.toContain('{"message"');
  });

  it('avisa si el enlace llega sin código y no llama al backend', () => {
    parametros = convertToParamMap({});

    const fixture = TestBed.createComponent(VisorEquipo);
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('El enlace no trae el código de un equipo');
  });
});
