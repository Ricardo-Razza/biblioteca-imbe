import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { catchError, map, of } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { ObraPublica, Pagina } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { PublicoApi } from '../publico-api';
import { disponibilidade } from '../textos';

const TAMANHO = 24;
const NA_ESTANTE = 6;

/**
 * Catálogo: estante com os últimos cadastrados (só na abertura, sem busca nem filtro), título, filtros e grade
 * de capas. A busca fica no cabeçalho do site. Tudo na URL (?q=&assunto=&disponiveis=&ordem=&pagina=).
 */
@Component({
  selector: 'app-catalogo',
  imports: [
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSlideToggleModule,
    CapaLivro,
  ],
  template: `
    @if (abertura() && novidades().length) {
      <section class="novidades" aria-labelledby="titulo-novidades">
        <div class="titulo-secao">
          <h2 id="titulo-novidades">Chegaram à estante</h2>
          <p>Os últimos livros cadastrados no acervo</p>
          <a routerLink="/" [queryParams]="{ ordem: 'recentes' }">Ver todos</a>
        </div>
        <ul class="estante">
          @for (o of novidades(); track o.id) {
            <li>
              <a [routerLink]="['/livro', o.id]" [attr.aria-label]="o.titulo">
                <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [titulo]="o.titulo" />
              </a>
            </li>
          }
        </ul>
      </section>
    }

    <section class="catalogo" aria-labelledby="titulo-catalogo">
      <div class="titulo-secao">
        <h1 id="titulo-catalogo">
          @if (q()) {
            Resultados para “{{ q() }}”
          } @else {
            Catálogo
          }
        </h1>
      </div>

      <div class="filtros">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="campo">
          <mat-label>Assunto</mat-label>
          <mat-select [value]="assuntoId()" (valueChange)="navegar({ assunto: $event, pagina: null })">
            <mat-option [value]="null">Todos</mat-option>
            @for (a of assuntos(); track a.id) {
              <mat-option [value]="a.id">{{ a.nome }} ({{ a.obras }})</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="campo curto">
          <mat-label>Ordenar</mat-label>
          <mat-select [value]="ordemAtual()" (valueChange)="navegar({ ordem: $event === 'recentes' ? 'recentes' : null, pagina: null })">
            <mat-option value="titulo">Por título</mat-option>
            <mat-option value="recentes">Mais recentes</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-slide-toggle [checked]="soDisponiveis()" (change)="navegar({ disponiveis: $event.checked ? 1 : null, pagina: null })">
          Disponíveis agora
        </mat-slide-toggle>
        @if (resultado(); as r) {
          <p class="contagem" role="status">
            @if (r.total === 0) {
              Nenhum livro encontrado
            } @else {
              {{ r.total === 1 ? '1 livro' : r.total + ' livros' }}
            }
          </p>
        }
      </div>

      @if (carregando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      @if (erro()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
      }

      @if (resultado(); as r) {
        @if (r.total === 0) {
          <p class="vazio">Tente outras palavras ou tire os filtros.</p>
        }
        <ul class="grade">
          @for (o of r.itens; track o.id) {
            <li>
              <a class="cartao" [routerLink]="['/livro', o.id]">
                <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [mini]="true" [titulo]="o.titulo" />
                <span class="titulo">{{ o.titulo }}</span>
                @if (o.autores.length) {
                  <span class="autor">{{ o.autores.join(', ') }}</span>
                }
                <span class="disponivel" [class.nao]="o.disponiveis <= 0">{{ disponivel(o.disponiveis) }}</span>
              </a>
            </li>
          }
        </ul>
        @if (r.totalPaginas > 1) {
          <nav class="paginas" aria-label="Páginas do catálogo">
            <button mat-stroked-button type="button" [disabled]="r.pagina === 0" (click)="irPara(r.pagina - 1)">
              <mat-icon>chevron_left</mat-icon> Anterior
            </button>
            <span>Página {{ r.pagina + 1 }} de {{ r.totalPaginas }}</span>
            <button mat-stroked-button type="button" [disabled]="r.pagina + 1 >= r.totalPaginas" (click)="irPara(r.pagina + 1)">
              Próxima <mat-icon iconPositionEnd>chevron_right</mat-icon>
            </button>
          </nav>
        }
      }
    </section>
  `,
  styles: `
    .titulo-secao {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 4px 14px;
      margin-bottom: 18px;
    }
    h1,
    h2 {
      min-width: 0;
      overflow-wrap: anywhere;
      font-size: clamp(22px, 3.2vw, 28px);
      line-height: 1.2;
    }
    .titulo-secao p {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
    .titulo-secao a {
      margin-left: auto;
      font: var(--mat-sys-body-medium);
      text-decoration: none;
    }

    /* Prateleira: capas em pé sobre uma tábua. */
    .novidades {
      margin-bottom: 44px;
    }
    .estante {
      list-style: none;
      margin: 0;
      padding: 0 8px;
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: clamp(12px, 2.4vw, 28px);
      align-items: end;
      border-bottom: 10px solid color-mix(in srgb, var(--marca-marinho) 14%, var(--papel));
      box-shadow: 0 6px 0 -2px color-mix(in srgb, var(--marca-marinho) 22%, var(--papel));
    }
    .estante a {
      display: block;
      --largura-capa: 100%;
      border-radius: 2px;
      transition: transform 0.15s;
    }
    .estante a:hover,
    .estante a:focus-visible {
      transform: translateY(-4px);
    }
    .estante app-capa-livro {
      border-radius: 2px;
      box-shadow:
        0 2px 2px rgb(0 0 0 / 0.15),
        6px 6px 14px rgb(31 48 95 / 0.18);
    }

    .filtros {
      min-width: 0;
      max-width: 100%;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px 16px;
      margin-bottom: 22px;
      padding-bottom: 18px;
      border-bottom: 1px solid var(--filete);
    }
    .campo {
      width: 240px;
      max-width: 100%;
    }
    .campo.curto {
      width: 190px;
    }
    .contagem {
      margin: 0 0 0 auto;
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .vazio {
      margin: 0 0 16px;
      color: var(--mat-sys-on-surface-variant);
    }
    .grade {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(160px, 100%), 1fr));
      gap: 22px 20px;
    }
    .grade > li {
      min-width: 0;
    }
    /* Cartão branco com sombra leve; capa com sombra própria, título em serifa, disponibilidade em texto. */
    .cartao {
      min-width: 0;
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 12px 12px 14px;
      border-radius: 4px;
      background: var(--mat-sys-surface);
      box-shadow: var(--sombra-leve);
      color: inherit;
      text-decoration: none;
      transition:
        box-shadow 0.15s,
        transform 0.15s;
      --largura-capa: 100%;
    }
    .cartao:hover,
    .cartao:focus-visible {
      box-shadow: var(--sombra-media);
      transform: translateY(-2px);
    }
    .cartao app-capa-livro {
      border-radius: 2px;
      box-shadow: 0 1px 2px rgb(0 0 0 / 0.2);
    }
    .titulo {
      margin-top: 12px;
      font-family: var(--fonte-titulo);
      font-size: 16px;
      font-weight: 500;
      line-height: 1.3;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .autor {
      margin-top: 4px;
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .disponivel {
      margin-top: auto;
      padding-top: 10px;
      display: flex;
      align-items: center;
      gap: 6px;
      font: var(--mat-sys-body-small);
      color: var(--situacao-ok);
    }
    .disponivel::before {
      content: '';
      flex: none;
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--prefeitura-verde);
    }
    .disponivel.nao {
      color: var(--mat-sys-on-surface-variant);
    }
    .disponivel.nao::before {
      background: var(--filete-forte);
    }
    .paginas {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-top: 28px;
    }
    @media (max-width: 600px) {
      .estante {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
      .estante li:nth-child(n + 4) {
        display: none;
      }
      .titulo-secao p {
        order: 3;
        width: 100%;
      }
      .contagem {
        width: 100%;
        margin-left: 0;
      }
    }
    @media (max-width: 480px) {
      .grade {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px 12px;
      }
      .cartao {
        padding: 10px 10px 12px;
      }
      .titulo {
        font-size: 15px;
      }
      .campo,
      .campo.curto {
        width: calc(50% - 8px);
        min-width: 0;
      }
    }
    @media (max-width: 340px) {
      .campo,
      .campo.curto {
        width: 100%;
      }
    }
  `,
})
export class Catalogo {
  readonly q = input<string>();
  readonly assunto = input<string>();
  readonly disponiveis = input<string>();
  readonly ordem = input<string>();
  readonly pagina = input<string>();

  private readonly api = inject(PublicoApi);
  private readonly router = inject(Router);

  protected readonly assuntos = toSignal(this.api.assuntos().pipe(catchError(() => of([]))), { initialValue: [] });
  /** Últimos cadastrados, para a estante da abertura (falha não atrapalha o catálogo). */
  protected readonly novidades = toSignal(
    this.api.obras({ ordem: 'recentes', tamanho: NA_ESTANTE }).pipe(
      map((p) => p.itens),
      catchError(() => of([] as ObraPublica[])),
    ),
    { initialValue: [] as ObraPublica[] },
  );
  protected readonly assuntoId = computed(() => (this.assunto() ? Number(this.assunto()) : null));
  protected readonly soDisponiveis = computed(() => !!this.disponiveis());
  protected readonly ordemAtual = computed(() => (this.ordem() === 'recentes' ? 'recentes' : 'titulo'));
  protected readonly paginaAtual = computed(() => Math.max(0, Number(this.pagina() ?? 0) || 0));
  /** Catálogo aberto sem busca, filtro nem página: aí aparece a estante. */
  protected readonly abertura = computed(
    () => !this.q() && this.assuntoId() == null && !this.soDisponiveis() && this.ordemAtual() === 'titulo' && this.paginaAtual() === 0,
  );
  protected readonly resultado = signal<Pagina<ObraPublica> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly disponivel = disponibilidade;

  constructor() {
    effect((aoLimpar) => {
      const filtro = {
        q: this.q(),
        assunto: this.assuntoId(),
        disponiveis: this.soDisponiveis(),
        ordem: this.ordemAtual(),
        pagina: this.paginaAtual(),
        tamanho: TAMANHO,
      } as const;
      this.carregando.set(true);
      this.erro.set(null);
      const s = this.api.obras(filtro).subscribe({
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
  }

  protected irPara(pagina: number): void {
    this.navegar({ pagina: pagina || null });
    window.scrollTo({ top: 0 });
  }

  protected navegar(params: Record<string, string | number | null>): void {
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }
}
