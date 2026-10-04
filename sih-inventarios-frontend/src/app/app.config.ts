import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import {
  PreloadAllModules,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
  withPreloading,
} from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // Sin esto, recargar el navegador te deja en el tope de la página
      // aunque la URL traiga #objetivos.
      withInMemoryScrolling({
        anchorScrolling: 'enabled',
        scrollPositionRestoration: 'enabled',
      }),
      // Descarga los chunks de los dashboards en segundo plano para que
      // entrar al panel tras el login sea inmediato y no espere una descarga.
      withPreloading(PreloadAllModules),
    ),
    // `withFetch()` hace que HttpClient use la API fetch nativa. Es lo
    // recomendado en SSR/prerender: el motor de Node no tiene XMLHttpRequest.
    // `withInterceptors` inyecta el JWT en las peticiones protegidas.
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    provideClientHydration(withEventReplay()),
  ],
};
