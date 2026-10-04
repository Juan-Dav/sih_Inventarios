import { TestBed } from '@angular/core/testing';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the main sections', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    const selectors = [
      'app-navbar',
      'app-hero',
      'app-quienes-somos',
      'app-mision-vision',
      'app-objetivos',
      'app-integrantes',
      'app-footer',
    ];
    const missing = selectors.filter((selector) => !compiled.querySelector(selector));

    expect(missing).toEqual([]);
  });
});
