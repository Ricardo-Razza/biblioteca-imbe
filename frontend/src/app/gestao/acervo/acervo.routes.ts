import { Routes } from '@angular/router';

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
