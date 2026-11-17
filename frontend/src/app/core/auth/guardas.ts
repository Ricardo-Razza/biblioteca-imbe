import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { Sessao } from './sessao';

/** `/painel/**` só com sessão; com senha provisória, só a tela de troca. */
export const guardaSessao: CanActivateFn = (_rota, estado) => {
  const sessao = inject(Sessao);
  const router = inject(Router);
  return sessao.carregar().pipe(
    map((f) => {
      if (!f) {
        return router.createUrlTree(['/login'], { queryParams: { volta: estado.url } });
      }
      if (f.trocarSenha && !estado.url.startsWith('/painel/senha')) {
        return router.createUrlTree(['/painel/senha']);
      }
      return true;
    }),
  );
};

/** Áreas só do administrador (Configurações). */
export const guardaAdmin: CanActivateFn = () => {
  const sessao = inject(Sessao);
  return sessao.admin() || inject(Router).createUrlTree(['/painel']);
};

/** Já logado não vê o login de novo. */
export const guardaLogin: CanActivateFn = () => {
  const sessao = inject(Sessao);
  const router = inject(Router);
  return sessao.carregar().pipe(map((f) => (f ? router.createUrlTree(['/painel']) : true)));
};
