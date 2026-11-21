import { Routes } from '@angular/router';

export const LEITORES_ROUTES: Routes = [
  {
    path: '',
    title: 'Leitores · Sala Literária Castelinho',
    loadComponent: () => import('./lista-leitores').then((m) => m.ListaLeitores),
  },
  {
    path: 'novo',
    title: 'Cadastrar leitor · Sala Literária Castelinho',
    loadComponent: () => import('./formulario-leitor').then((m) => m.FormularioLeitor),
  },
  {
    path: ':id',
    title: 'Leitor · Sala Literária Castelinho',
    loadComponent: () => import('./ficha-leitor').then((m) => m.FichaLeitor),
  },
  {
    path: ':id/editar',
    title: 'Editar leitor · Sala Literária Castelinho',
    loadComponent: () => import('./formulario-leitor').then((m) => m.FormularioLeitor),
  },
];
