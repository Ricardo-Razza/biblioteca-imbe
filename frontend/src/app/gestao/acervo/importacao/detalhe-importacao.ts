import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../../core/api/erro-api';
import { Pagina } from '../../../core/api/modelos';
import { PaginadorPtBr } from '../../../core/paginador-pt-br';
import { Selo } from '../../../shared/selo/selo';
import {
  Importacao,
  ImportacaoApi,
  ItemImportacao,
  RESULTADOS,
  ResultadoItem,
  progresso,
  textoResultado,
} from './importacao-api';

const ATUALIZACAO_MS = 3000;

/**
 * Uma importação: progresso (atualiza sozinho enquanto processa), contagem por resultado e as linhas, com filtro.
 * Linhas com fonte fora do ar podem ir de novo para a fila.
 */
@Component({
  selector: 'app-detalhe-importacao',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatPaginatorModule, MatProgressBarModule, Selo],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <a mat-button routerLink="/painel/acervo/importar" class="voltar"><mat-icon>arrow_back</mat-icon> Importações</a>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }

    @if (dados(); as d) {
      <h1>{{ d.arquivo }}</h1>
      <p class="texto-secundario">
        Enviada em {{ d.criadaEm | date: 'dd/MM/yyyy HH:mm' }}{{ d.funcionario ? ' por ' + d.funcionario : '' }} ·
        {{ d.total }} {{ d.total === 1 ? 'linha' : 'linhas' }}
        @if (d.finalizadaEm) {
          · concluída em {{ d.finalizadaEm | date: 'dd/MM/yyyy HH:mm' }}
        }
      </p>

      @if (d.status === 'PROCESSANDO') {
        <div class="progresso" role="status">
          <mat-progress-bar mode="determinate" [value]="pct(d)" />
          <span>Processando: {{ d.total - d.pendentes }} de {{ d.total }} ({{ pct(d) }}%). Pode sair desta tela, o trabalho continua.</span>
        </div>
      }

      <div class="filtros" role="group" aria-label="Filtrar linhas por resultado">
        <button mat-stroked-button type="button" [class.ativo]="!filtro()" (click)="filtrar(null)">Todas ({{ d.total }})</button>
        @for (r of resultados; track r.valor) {
          @if (d.contagem[r.valor] > 0) {
            <button mat-stroked-button type="button" [class.ativo]="filtro() === r.valor" (click)="filtrar(r.valor)">
              {{ r.texto }} ({{ d.contagem[r.valor] }})
            </button>
          }
        }
      </div>

      @if (d.status === 'CONCLUIDA' && d.contagem.FONTE_INDISPONIVEL > 0) {
        <p class="aviso alerta">
          <mat-icon>cloud_off</mat-icon>
          <span>
            {{ d.contagem.FONTE_INDISPONIVEL === 1 ? '1 linha não pôde ser consultada' : d.contagem.FONTE_INDISPONIVEL + ' linhas não puderam ser consultadas' }}
            porque uma fonte estava fora do ar.
            <button mat-button type="button" (click)="tentarDeNovo(d)" [disabled]="ocupado()">Tentar de novo</button>
          </span>
        </p>
      }

      @if (itens(); as p) {
        <ul class="lista">
          @for (i of p.itens; track i.id) {
            <li>
              <span class="linha texto-secundario">Linha {{ i.linha }}</span>
              <div class="info">
                @if (i.obraId) {
                  <a [routerLink]="['/painel/acervo', i.obraId]">{{ i.titulo }}</a>
                }
                <span>
                  ISBN {{ i.isbn13 ?? i.isbnOriginal }}{{ i.tombo ? ' · tombo ' + i.tombo : '' }}{{ i.localizacao ? ' · ' + i.localizacao : '' }}
                </span>
                @if (i.mensagem) {
                  <span class="texto-secundario">{{ i.mensagem }}</span>
                }
              </div>
              <app-selo [tom]="selo(i.resultado).tom">{{ selo(i.resultado).texto }}</app-selo>
            </li>
          }
        </ul>
        @if (p.total > p.tamanho) {
          <mat-paginator [length]="p.total" [pageIndex]="p.pagina" [pageSize]="p.tamanho" [hidePageSize]="true" (page)="mudarPagina($event)" />
        }
      }
    }
  `,
  styles: `
    .voltar {
      margin-left: -12px;
    }
    h1 {
      word-break: break-all;
      margin-bottom: 4px;
    }
    .progresso {
      display: flex;
      flex-direction: column;
      gap: 6px;
      max-width: 560px;
      margin: 16px 0;
    }
    .filtros {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 16px 0;
    }
    .filtros .ativo {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    .aviso {
      max-width: 760px;
    }
    .lista {
      list-style: none;
      padding: 0;
      max-width: 900px;
    }
    .lista li {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 10px 4px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .linha {
      min-width: 72px;
    }
    .info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .info a {
      font-weight: 500;
      color: inherit;
      text-decoration: none;
    }
    .info a:hover {
      text-decoration: underline;
    }
    @media (max-width: 600px) {
      .lista li {
        flex-wrap: wrap;
      }
    }
  `,
})
export class DetalheImportacao {
  readonly importacao = input.required<string>();

  private readonly api = inject(ImportacaoApi);
  protected readonly resultados = RESULTADOS;
  protected readonly dados = signal<Importacao | null>(null);
  protected readonly itens = signal<Pagina<ItemImportacao> | null>(null);
  protected readonly filtro = signal<ResultadoItem | null>(null);
  protected readonly pagina = signal(0);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly pct = progresso;
  protected readonly selo = textoResultado;
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect(() => {
      this.importacao();
      this.carregar();
    });
    effect((aoLimpar) => {
      const s = this.api.itens(this.id(), this.filtro(), this.pagina()).subscribe({
        next: (p) => this.itens.set(p),
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
      aoLimpar(() => s.unsubscribe());
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
  }

  private id(): number {
    return Number(this.importacao());
  }

  /** Busca o resumo; enquanto processa, de novo a cada 3 s (com as linhas da página atual). */
  private carregar(): void {
    clearTimeout(this.espera);
    this.api.buscar(this.id()).subscribe({
      next: (d) => {
        const estavaProcessando = this.dados()?.status === 'PROCESSANDO';
        this.dados.set(d);
        if (d.status === 'PROCESSANDO') {
          this.espera = setTimeout(() => {
            this.carregar();
            this.recarregarItens();
          }, ATUALIZACAO_MS);
        } else if (estavaProcessando) {
          this.recarregarItens();
        }
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private recarregarItens(): void {
    this.api.itens(this.id(), this.filtro(), this.pagina()).subscribe((p) => this.itens.set(p));
  }

  protected filtrar(r: ResultadoItem | null): void {
    this.filtro.set(r);
    this.pagina.set(0);
  }

  protected mudarPagina(e: PageEvent): void {
    this.pagina.set(e.pageIndex);
  }

  protected tentarDeNovo(d: Importacao): void {
    this.ocupado.set(true);
    this.erro.set(null);
    this.api.tentarDeNovo(d.id).subscribe({
      next: (n) => {
        this.ocupado.set(false);
        this.mensagem.set(`${n.pendentes} ${n.pendentes === 1 ? 'linha voltou' : 'linhas voltaram'} para a fila.`);
        this.dados.set(n);
        this.carregar();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}
