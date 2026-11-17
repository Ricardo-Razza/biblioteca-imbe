import { Routes } from '@angular/router';

import { guardaAdmin, guardaLogin, guardaSessao } from './core/auth/guardas';

const emBreve = (path: string, titulo: string, fase: number) => ({
  path,
  title: `${titulo} · Biblioteca de Imbé`,
  loadComponent: () => import('./gestao/em-breve').then((m) => m.EmBreve),
  data: { titulo, fase },
});

export const routes: Routes = [
  // Site público na fase 4. Até lá, a raiz abre a gestão.
  { path: '', pathMatch: 'full', redirectTo: 'painel' },
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
      { path: '', pathMatch: 'full', redirectTo: 'acervo' },
      emBreve('inicio', 'Início', 5),
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
      emBreve('reservas', 'Reservas', 4),
      emBreve('relatorios', 'Relatórios', 5),
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
  { path: '**', redirectTo: 'painel' },
];
