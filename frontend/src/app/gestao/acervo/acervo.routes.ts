import { Routes } from '@angular/router';

import { guardaAdmin } from '../../core/auth/guardas';

export const ACERVO_ROUTES: Routes = [
  {
    path: '',
    title: 'Acervo · Sala Literária Castelinho',
    loadComponent: () => import('./lista-acervo').then((m) => m.ListaAcervo),
  },
  {
    path: 'novo',
    title: 'Cadastrar livro · Sala Literária Castelinho',
    loadComponent: () => import('./cadastro-obra').then((m) => m.CadastroObra),
  },
  {
    path: 'importar',
    title: 'Importar planilha · Sala Literária Castelinho',
    canActivate: [guardaAdmin],
    loadComponent: () => import('./importacao/importacoes').then((m) => m.Importacoes),
  },
  {
    path: 'importar/:importacao',
    title: 'Importação · Sala Literária Castelinho',
    canActivate: [guardaAdmin],
    loadComponent: () => import('./importacao/detalhe-importacao').then((m) => m.DetalheImportacao),
  },
  {
    path: ':id',
    title: 'Obra · Sala Literária Castelinho',
    loadComponent: () => import('./detalhe-obra').then((m) => m.DetalheObra),
  },
  {
    path: ':id/editar',
    title: 'Editar obra · Sala Literária Castelinho',
    loadComponent: () => import('./edicao-obra').then((m) => m.EdicaoObra),
  },
];
