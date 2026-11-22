import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS, MatFormFieldDefaultOptions } from '@angular/material/form-field';
import { MatIconRegistry } from '@angular/material/icon';

import { routes } from './app.routes';
import { interceptorAuth } from './core/auth/interceptor-auth';
import { provideRolagem } from './core/rolagem';

registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Rolagem decidida em provideRolagem (topo ao mudar de página; posição antiga ao voltar).
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'disabled' })),
    provideRolagem(),
    provideHttpClient(withFetch(), withInterceptors([interceptorAuth])),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    // Dica e erro do campo ocupam a altura que precisarem: com a altura fixa do Material, texto de duas linhas
    // (comum no celular) invadia o campo de baixo. O espaço entre campos vem do gap do formulário (.coluna).
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { subscriptSizing: 'dynamic' } satisfies MatFormFieldDefaultOptions },
    // Material Symbols (index.html) no lugar da fonte antiga Material Icons.
    provideAppInitializer(() => {
      inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
    }),
  ],
};
