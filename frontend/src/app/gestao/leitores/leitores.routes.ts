import { Routes } from '@angular/router';

export const LEITORES_ROUTES: Routes = [
  {
    path: '',
    title: 'Leitores · Biblioteca de Imbé',
    loadComponent: () => import('./lista-leitores').then((m) => m.ListaLeitores),
  },
  {
    path: 'novo',
    title: 'Cadastrar leitor · Biblioteca de Imbé',
    loadComponent: () => import('./formulario-leitor').then((m) => m.FormularioLeitor),
  },
  {
    path: ':id',
    title: 'Leitor · Biblioteca de Imbé',
    loadComponent: () => import('./ficha-leitor').then((m) => m.FichaLeitor),
  },
  {
    path: ':id/editar',
    title: 'Editar leitor · Biblioteca de Imbé',
    loadComponent: () => import('./formulario-leitor').then((m) => m.FormularioLeitor),
  },
];
