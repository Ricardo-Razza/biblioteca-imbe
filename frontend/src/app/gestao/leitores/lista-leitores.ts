import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Leitor, Pagina } from '../../core/api/modelos';
import { PaginadorPtBr } from '../../core/paginador-pt-br';
import { dataBr, formatarCpf } from '../../shared/formatos';
import { Selo } from '../../shared/selo/selo';
import { LeitoresApi } from './leitores-api';

const TAMANHO = 20;

@Component({
  selector: 'app-lista-leitores',
  imports: [
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    Selo,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <div class="cabecalho">
      <h1>Leitores</h1>
      <a mat-flat-button routerLink="novo"><mat-icon>person_add</mat-icon> Cadastrar leitor</a>
    </div>

    <mat-form-field appearance="outline" class="busca" subscriptSizing="dynamic">
      <mat-label>Buscar</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [value]="q() ?? ''" (input)="digitar($any($event.target).value)" autocomplete="off" />
      <mat-hint>Nome ou CPF.</mat-hint>
    </mat-form-field>

    @if (carregando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    @if (resultado(); as r) {
      @if (r.total === 0) {
        <p class="texto-secundario vazio">
          {{ q() ? 'Nenhum leitor encontrado para "' + q() + '".' : 'Nenhum leitor cadastrado ainda.' }}
        </p>
      } @else {
        <ul class="lista">
          @for (l of r.itens; track l.id) {
            <li>
              <a class="linha" [routerLink]="[l.id]">
                <span class="info">
                  <span class="nome">{{ l.nome }}</span>
                  <span class="texto-secundario">CPF {{ cpf(l.cpf) }}</span>
                </span>
                <span class="selos">
                  @if (!l.ativo) {
                    <app-selo tom="neutra">Desativado</app-selo>
                  }
                  @if (l.bloqueadoAte) {
                    <app-selo tom="erro">Bloqueado até {{ data(l.bloqueadoAte) }}</app-selo>
                  }
                  @if (l.menorDeIdade) {
                    <app-selo tom="neutra">Menor de idade</app-selo>
                  }
                </span>
              </a>
            </li>
          }
        </ul>
        <mat-paginator [length]="r.total" [pageIndex]="r.pagina" [pageSize]="tamanho" [hidePageSize]="true" (page)="mudarPagina($event)" />
      }
    }
  `,
  styles: `
    .cabecalho {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }
    .busca {
      width: 100%;
      max-width: 640px;
      margin-bottom: 12px;
    }
    .vazio {
      margin-top: 16px;
    }
    .lista {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .linha {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      color: inherit;
      text-decoration: none;
    }
    .linha:hover {
      background: var(--mat-sys-surface-container);
    }
    .info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .nome {
      font-weight: 500;
    }
    .selos {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      justify-content: flex-end;
    }
  `,
})
export class ListaLeitores {
  readonly q = input<string>();
  readonly pagina = input<string>();

  private readonly api = inject(LeitoresApi);
  private readonly router = inject(Router);
  protected readonly tamanho = TAMANHO;
  protected readonly resultado = signal<Pagina<Leitor> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly cpf = formatarCpf;
  protected readonly data = dataBr;
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect((aoLimpar) => {
      const pagina = Math.max(0, Number(this.pagina() ?? 0) || 0);
      this.carregando.set(true);
      this.erro.set(null);
      const s = this.api.listar(this.q() ?? '', pagina, TAMANHO).subscribe({
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

  protected digitar(texto: string): void {
    clearTimeout(this.espera);
    this.espera = setTimeout(() => this.navegar(texto, 0), 300);
  }

  protected mudarPagina(e: PageEvent): void {
    this.navegar(this.q() ?? '', e.pageIndex);
  }

  private navegar(q: string, pagina: number): void {
    this.router.navigate([], { queryParams: { q: q.trim() ? q : null, pagina: pagina || null }, replaceUrl: true });
  }
}
