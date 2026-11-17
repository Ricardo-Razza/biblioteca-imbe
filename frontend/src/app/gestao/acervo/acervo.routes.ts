import { Routes } from '@angular/router';

import { guardaAdmin } from '../../core/auth/guardas';

export const ACERVO_ROUTES: Routes = [
  {
    path: '',
    title: 'Acervo · Biblioteca de Imbé',
    loadComponent: () => import('./lista-acervo').then((m) => m.ListaAcervo),
  },
  {
    path: 'novo',
    title: 'Cadastrar livro · Biblioteca de Imbé',
    loadComponent: () => import('./cadastro-obra').then((m) => m.CadastroObra),
  },
  {
    path: 'importar',
    title: 'Importar planilha · Biblioteca de Imbé',
    canActivate: [guardaAdmin],
    loadComponent: () => import('./importacao/importacoes').then((m) => m.Importacoes),
  },
  {
    path: 'importar/:importacao',
    title: 'Importação · Biblioteca de Imbé',
    canActivate: [guardaAdmin],
    loadComponent: () => import('./importacao/detalhe-importacao').then((m) => m.DetalheImportacao),
  },
  {
    path: ':id',
    title: 'Obra · Biblioteca de Imbé',
    loadComponent: () => import('./detalhe-obra').then((m) => m.DetalheObra),
  },
  {
    path: ':id/editar',
    title: 'Editar obra · Biblioteca de Imbé',
    loadComponent: () => import('./edicao-obra').then((m) => m.EdicaoObra),
  },
];
