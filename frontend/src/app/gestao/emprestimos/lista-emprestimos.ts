import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Emprestimo, Pagina, StatusEmprestimo } from '../../core/api/modelos';
import { PaginadorPtBr } from '../../core/paginador-pt-br';
import { dataBr } from '../../shared/formatos';
import { EmprestimosApi } from './emprestimos-api';
import { ItemEmprestimo } from './item-emprestimo';

const ABAS: { status: StatusEmprestimo; texto: string }[] = [
  { status: 'ATIVO', texto: 'Ativos' },
  { status: 'ATRASADO', texto: 'Atrasados' },
  { status: 'DEVOLVIDO', texto: 'Histórico' },
];
const TAMANHO = 20;

/** Empréstimos em abas: Ativos (todos em aberto), Atrasados e Histórico. Filtros na URL. */
@Component({
  selector: 'app-lista-emprestimos',
  imports: [
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTabsModule,
    ItemEmprestimo,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <h1>Empréstimos</h1>

    <mat-tab-group [selectedIndex]="indice()" (selectedIndexChange)="trocarAba($event)" mat-stretch-tabs="false" animationDuration="0ms">
      @for (a of abas; track a.status) {
        <mat-tab [label]="a.texto" />
      }
    </mat-tab-group>

    <mat-form-field appearance="outline" class="busca" subscriptSizing="dynamic">
      <mat-label>Filtrar</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [value]="q() ?? ''" (input)="digitar($any($event.target).value)" autocomplete="off" maxlength="100" />
      <mat-hint>Título do livro ou nome do leitor.</mat-hint>
    </mat-form-field>

    @if (carregando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }

    @if (resultado(); as r) {
      @if (r.total === 0) {
        <p class="texto-secundario vazio">Nada por aqui.</p>
      } @else {
        @for (e of r.itens; track e.id) {
          <app-item-emprestimo [e]="e">
            @if (e.status === 'ATIVO' && e.renovacoesRestantes > 0) {
              <button mat-button type="button" (click)="renovar(e)" [disabled]="ocupado()">Renovar</button>
            }
            @if (e.status !== 'DEVOLVIDO') {
              <a mat-stroked-button routerLink="/painel/devolver" [queryParams]="{ tombo: e.exemplar.tombo }">Devolver</a>
            }
          </app-item-emprestimo>
        }
        <mat-paginator [length]="r.total" [pageIndex]="r.pagina" [pageSize]="tamanho" [hidePageSize]="true" (page)="mudarPagina($event)" />
      }
    }
  `,
  styles: `
    h1 {
      margin-bottom: 8px;
    }
    .busca {
      width: 100%;
      max-width: 640px;
      margin: 16px 0 12px;
    }
    .vazio {
      margin-top: 16px;
    }
    .aviso {
      max-width: 760px;
      margin: 8px 0;
    }
  `,
})
export class ListaEmprestimos {
  readonly status = input<string>();
  readonly q = input<string>();
  readonly pagina = input<string>();

  private readonly api = inject(EmprestimosApi);
  private readonly router = inject(Router);
  protected readonly abas = ABAS;
  protected readonly tamanho = TAMANHO;
  protected readonly indice = computed(() => Math.max(0, ABAS.findIndex((a) => a.status === this.status())));
  protected readonly resultado = signal<Pagina<Emprestimo> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  private readonly recarga = signal(0);
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect((aoLimpar) => {
      this.recarga();
      const status = ABAS[this.indice()].status;
      const pagina = Math.max(0, Number(this.pagina() ?? 0) || 0);
      this.carregando.set(true);
      this.erro.set(null);
      const s = this.api.listar({ status, q: this.q(), pagina, tamanho: TAMANHO }).subscribe({
        next: (r) => {
          this.carregando.set(false);
          this.resultado.set(r);
        },
        error: (e) => {
          this.carregando.set(false);
          this.erro.set(mensagemDeErro(e));
        },
      });
      aoLimpar(() => s.unsubscribe());
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
  }

  protected trocarAba(i: number): void {
    this.mensagem.set(null);
    this.navegar({ status: i === 0 ? null : ABAS[i].status, pagina: null });
  }

  protected digitar(texto: string): void {
    clearTimeout(this.espera);
    this.espera = setTimeout(() => this.navegar({ q: texto.trim() ? texto : null, pagina: null }), 300);
  }

  protected mudarPagina(e: PageEvent): void {
    this.navegar({ pagina: e.pageIndex || null });
  }

  protected renovar(e: Emprestimo): void {
    this.ocupado.set(true);
    this.erro.set(null);
    this.mensagem.set(null);
    this.api.renovar(e.id).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.mensagem.set(`"${r.exemplar.titulo}" renovado. Nova devolução em ${dataBr(r.dataPrevista)}.`);
        this.recarga.update((n) => n + 1);
      },
      error: (err) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(err));
      },
    });
  }

  private navegar(params: Record<string, string | number | null>): void {
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge', replaceUrl: true });
  }
}
