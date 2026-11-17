import { Routes } from '@angular/router';

const emBreve = (path: string, titulo: string, fase: number) => ({
  path,
  title: `${titulo} · Biblioteca de Imbé`,
  loadComponent: () => import('./gestao/em-breve').then((m) => m.EmBreve),
  data: { titulo, fase },
});

export const routes: Routes = [
  // Site público na fase 4; login na fase 3. Até lá, a raiz abre a gestão.
  { path: '', pathMatch: 'full', redirectTo: 'painel' },
  {
    path: 'painel',
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
      emBreve('configuracoes', 'Configurações', 3),
    ],
  },
  { path: '**', redirectTo: 'painel' },
];
