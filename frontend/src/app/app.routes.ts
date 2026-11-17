import { Routes } from '@angular/router';

import { guardaAdmin, guardaLogin, guardaSessao } from './core/auth/guardas';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./publico/layout-publico').then((m) => m.LayoutPublico),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Catálogo · Biblioteca de Imbé',
        loadComponent: () => import('./publico/catalogo/catalogo').then((m) => m.Catalogo),
      },
      {
        path: 'livro/:id',
        title: 'Livro · Biblioteca de Imbé',
        loadComponent: () => import('./publico/livro/livro').then((m) => m.Livro),
      },
      {
        path: 'reservar/:id',
        title: 'Reservar · Biblioteca de Imbé',
        loadComponent: () => import('./publico/reservar/reservar').then((m) => m.Reservar),
      },
      {
        path: 'minhas-reservas',
        title: 'Minhas reservas · Biblioteca de Imbé',
        loadComponent: () => import('./publico/minhas-reservas/minhas-reservas').then((m) => m.MinhasReservas),
      },
    ],
  },
  {
    path: 'login',
    title: 'Entrar · Biblioteca de Imbé',
    canActivate: [guardaLogin],
    loadComponent: () => import('./gestao/conta/login').then((m) => m.Login),
  },
  {
    path: 'painel',
    canActivate: [guardaSessao],
    runGuardsAndResolvers: 'always',
    loadComponent: () => import('./gestao/layout-gestao').then((m) => m.LayoutGestao),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        title: 'Início · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'acervo',
        loadChildren: () => import('./gestao/acervo/acervo.routes').then((m) => m.ACERVO_ROUTES),
      },
      {
        path: 'leitores',
        loadChildren: () => import('./gestao/leitores/leitores.routes').then((m) => m.LEITORES_ROUTES),
      },
      {
        path: 'emprestimos',
        title: 'Empréstimos · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/emprestimos/lista-emprestimos').then((m) => m.ListaEmprestimos),
      },
      {
        path: 'emprestar',
        title: 'Emprestar · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/balcao/emprestar').then((m) => m.Emprestar),
      },
      {
        path: 'devolver',
        title: 'Devolver · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/balcao/devolver').then((m) => m.Devolver),
      },
      {
        path: 'reservas',
        title: 'Reservas · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/reservas/reservas').then((m) => m.Reservas),
      },
      {
        path: 'relatorios',
        title: 'Relatórios · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/relatorios/relatorios').then((m) => m.Relatorios),
      },
      {
        path: 'configuracoes',
        title: 'Configurações · Biblioteca de Imbé',
        canActivate: [guardaAdmin],
        loadComponent: () => import('./gestao/configuracoes/configuracoes').then((m) => m.Configuracoes),
      },
      {
        path: 'senha',
        title: 'Trocar senha · Biblioteca de Imbé',
        loadComponent: () => import('./gestao/conta/troca-senha').then((m) => m.TrocaSenha),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
