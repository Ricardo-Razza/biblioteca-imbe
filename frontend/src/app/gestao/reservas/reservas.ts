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
import { Pagina, Reserva } from '../../core/api/modelos';
import { PaginadorPtBr } from '../../core/paginador-pt-br';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { dataBr } from '../../shared/formatos';
import { Selo } from '../../shared/selo/selo';
import { protocoloLegivel } from '../../publico/textos';
import { FINALIZADAS, ReservasApi, hojeIso, seloReserva } from './reservas-api';

const TAMANHO = 20;

/**
 * Reservas feitas pelo site. "Para separar": o que vence antes primeiro, com tombo e localização para buscar
 * na estante; Entregar abre o empréstimo com o mesmo exemplar. "Histórico": entregues, canceladas e expiradas.
 */
@Component({
  selector: 'app-reservas',
  imports: [
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTabsModule,
    CapaLivro,
    Selo,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <h1>Reservas</h1>

    <mat-tab-group [selectedIndex]="historico() ? 1 : 0" (selectedIndexChange)="trocarAba($event)" mat-stretch-tabs="false" animationDuration="0ms">
      <mat-tab label="Para separar" />
      <mat-tab label="Histórico" />
    </mat-tab-group>

    <mat-form-field appearance="outline" class="busca" subscriptSizing="dynamic">
      <mat-label>Filtrar</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [value]="q() ?? ''" (input)="digitar($any($event.target).value)" autocomplete="off" />
      <mat-hint>Título, nome do leitor, protocolo ou tombo.</mat-hint>
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
        <p class="texto-secundario vazio">{{ historico() ? 'Nada no histórico.' : 'Nenhuma reserva para separar.' }}</p>
      } @else {
        <ul class="lista">
          @for (x of r.itens; track x.id) {
            <li>
              <app-capa-livro [obraId]="x.obra.id" [temCapa]="x.obra.temCapa" [mini]="true" [titulo]="x.obra.titulo" />
              <div class="info">
                <a class="titulo" [routerLink]="['/painel/acervo', x.obra.id]">{{ x.obra.titulo }}</a>
                <span><strong>Tombo {{ x.exemplar.tombo }}</strong>{{ x.exemplar.localizacao ? ' · ' + x.exemplar.localizacao : '' }}</span>
                <span>
                  <a [routerLink]="['/painel/leitores', x.leitor.id]">{{ x.leitor.nome }}</a>
                  @if (x.leitor.telefone) {
                    <span class="texto-secundario"> · {{ x.leitor.telefone }}</span>
                  }
                </span>
                <span class="texto-secundario">Protocolo {{ legivel(x.protocolo) }} · feita em {{ data(x.criadaEm) }}</span>
                @if (cancelando() === x.id) {
                  <div class="aviso alerta confirmar" role="alertdialog" aria-label="Confirmar cancelamento">
                    <span>Cancelar a reserva? O exemplar {{ x.exemplar.tombo }} volta a ficar disponível.</span>
                    <div class="acoes">
                      <button mat-button type="button" (click)="cancelando.set(null)">Manter</button>
                      <button mat-flat-button type="button" [disabled]="ocupado()" (click)="cancelar(x)">Cancelar reserva</button>
                    </div>
                  </div>
                }
              </div>
              <app-selo [tom]="selo(x).tom">{{ selo(x).texto }}</app-selo>
              @if (x.status === 'AGUARDANDO_RETIRADA' && cancelando() !== x.id) {
                <div class="acoes-item">
                  <button mat-flat-button type="button" [disabled]="ocupado()" (click)="entregar(x)">Entregar</button>
                  <button mat-button type="button" [disabled]="ocupado()" (click)="cancelando.set(x.id)">Cancelar</button>
                </div>
              }
            </li>
          }
        </ul>
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
    .lista {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .lista li {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 10px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      --largura-capa: 48px;
    }
    .info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .titulo {
      font-weight: 500;
      color: inherit;
      text-decoration: none;
    }
    .info a {
      text-decoration: none;
    }
    .info a:hover {
      text-decoration: underline;
    }
    .confirmar {
      flex-direction: column;
      margin-top: 6px;
    }
    .acoes-item {
      display: flex;
      gap: 4px;
    }
    @media (max-width: 640px) {
      .lista li {
        flex-wrap: wrap;
      }
    }
  `,
})
export class Reservas {
  readonly aba = input<string>();
  readonly q = input<string>();
  readonly pagina = input<string>();

  private readonly api = inject(ReservasApi);
  private readonly router = inject(Router);
  protected readonly tamanho = TAMANHO;
  protected readonly historico = computed(() => this.aba() === 'historico');
  protected readonly resultado = signal<Pagina<Reserva> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly cancelando = signal<number | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly data = dataBr;
  protected readonly legivel = protocoloLegivel;
  private readonly hoje = hojeIso();
  private readonly recarga = signal(0);
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect((aoLimpar) => {
      this.recarga();
      const status = this.historico() ? FINALIZADAS : undefined;
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

  protected selo(r: Reserva) {
    return seloReserva(r, this.hoje);
  }

  protected trocarAba(i: number): void {
    this.mensagem.set(null);
    this.navegar({ aba: i === 1 ? 'historico' : null, pagina: null });
  }

  protected digitar(texto: string): void {
    clearTimeout(this.espera);
    this.espera = setTimeout(() => this.navegar({ q: texto.trim() ? texto : null, pagina: null }), 300);
  }

  protected mudarPagina(e: PageEvent): void {
    this.navegar({ pagina: e.pageIndex || null });
  }

  protected entregar(x: Reserva): void {
    this.executar(this.api.entregar(x.id), (r) =>
      `"${r.obra.titulo}" entregue a ${r.leitor.nome}. Devolução até ${dataBr(r.devolucaoPrevista)}.`,
    );
  }

  protected cancelar(x: Reserva): void {
    this.executar(this.api.cancelar(x.id), (r) => `Reserva ${protocoloLegivel(r.protocolo)} cancelada. O exemplar ${r.exemplar.tombo} voltou ao acervo.`);
  }

  private executar(acao: ReturnType<ReservasApi['entregar']>, texto: (r: Reserva) => string): void {
    this.ocupado.set(true);
    this.erro.set(null);
    this.mensagem.set(null);
    acao.subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.cancelando.set(null);
        this.mensagem.set(texto(r));
        this.recarga.update((n) => n + 1);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  private navegar(params: Record<string, string | number | null>): void {
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge', replaceUrl: true });
  }
}
