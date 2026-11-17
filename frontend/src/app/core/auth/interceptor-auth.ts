import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { Sessao } from './sessao';

const LOGIN = '/api/v1/auth/login';

/**
 * Manda o token nas chamadas da gestão. 401 fora do login: a sessão acabou (expirou ou o funcionário foi
 * desativado), então volta para o login guardando onde estava.
 */
export const interceptorAuth: HttpInterceptorFn = (req, next) => {
  const sessao = inject(Sessao);
  const router = inject(Router);
  const token = sessao.token();
  const daApi = req.url.startsWith('/api/v1/') && !req.url.startsWith('/api/v1/publico/') && req.url !== LOGIN;

  const pedido = token && daApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(pedido).pipe(
    catchError((erro: unknown) => {
      if (daApi && erro instanceof HttpErrorResponse && erro.status === 401) {
        sessao.sair();
        const volta = router.url.startsWith('/login') ? undefined : router.url;
        router.navigate(['/login'], { queryParams: { volta, expirou: 1 } });
      }
      return throwError(() => erro);
    }),
  );
};
