import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { catchError, of } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { ObraPublica, Pagina } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { Selo } from '../../shared/selo/selo';
import { PublicoApi } from '../publico-api';
import { disponibilidade } from '../textos';

const TAMANHO = 24;

/** Catálogo: título, filtros e grade de capas (a busca fica no cabeçalho do site). Tudo na URL (?q=&assunto=&disponiveis=&pagina=). */
@Component({
  selector: 'app-catalogo',
  imports: [
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    CapaLivro,
    Selo,
  ],
  template: `
    <section class="topo-catalogo" aria-labelledby="titulo-catalogo">
      <h1 id="titulo-catalogo">
        @if (q()) {
          Resultados para “{{ q() }}”
        } @else {
          Catálogo
        }
      </h1>
      <div class="filtros">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="assunto">
          <mat-label>Assunto</mat-label>
          <mat-select [value]="assuntoId()" (valueChange)="navegar({ assunto: $event, pagina: null })">
            <mat-option [value]="null">Todos</mat-option>
            @for (a of assuntos(); track a.id) {
              <mat-option [value]="a.id">{{ a.nome }} ({{ a.obras }})</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-checkbox [checked]="soDisponiveis()" (change)="navegar({ disponiveis: $event.checked ? 1 : null, pagina: null })">
          Disponíveis agora
        </mat-checkbox>
      </div>
    </section>

    @if (carregando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    @if (resultado(); as r) {
      <p class="contagem texto-secundario" role="status">
        @if (r.total === 0) {
          Nenhum livro encontrado. Tente outras palavras ou tire os filtros.
        } @else {
          {{ r.total === 1 ? '1 livro' : r.total + ' livros' }}
        }
      </p>
      <ul class="grade">
        @for (o of r.itens; track o.id) {
          <li>
            <a class="cartao" [routerLink]="['/livro', o.id]">
              <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [mini]="true" [titulo]="o.titulo" />
              <span class="titulo">{{ o.titulo }}</span>
              @if (o.autores.length) {
                <span class="autor texto-secundario">{{ o.autores.join(', ') }}</span>
              }
              <app-selo [tom]="o.disponiveis > 0 ? 'ok' : 'neutra'">{{ disponivel(o.disponiveis) }}</app-selo>
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
  `,
  styles: `
    /* Título e filtros num bloco próprio, destacado do fundo */
    .topo-catalogo {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px 24px;
      margin-bottom: 20px;
      padding: 16px 20px;
      border-radius: 12px;
      background: var(--mat-sys-surface);
      box-shadow: var(--sombra-leve);
    }
    h1 {
      min-width: 0;
      overflow-wrap: anywhere;
    }
    .filtros {
      min-width: 0;
      max-width: 100%;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px 24px;
    }
    .assunto {
      width: 260px;
      max-width: 100%;
    }
    .contagem {
      margin: 12px 0;
    }
    .grade {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(150px, 100%), 1fr));
      gap: 20px 16px;
    }
    .grade > li {
      min-width: 0;
    }
    /* min-width: 0 deixa o nome longo do autor ser cortado com reticências em vez de alargar a coluna */
    .cartao {
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 6px;
      height: 100%;
      padding: 10px 10px 12px;
      border-radius: 12px;
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
    .titulo {
      font-weight: 500;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .autor {
      font: var(--mat-sys-body-medium);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    app-selo {
      align-self: flex-start;
    }
    .paginas {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-top: 24px;
    }
    @media (max-width: 480px) {
      .grade {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px 10px;
      }
      .topo-catalogo {
        padding: 14px 16px;
      }
    }
  `,
})
export class Catalogo {
  readonly q = input<string>();
  readonly assunto = input<string>();
  readonly disponiveis = input<string>();
  readonly pagina = input<string>();

  private readonly api = inject(PublicoApi);
  private readonly router = inject(Router);

  protected readonly assuntos = toSignal(this.api.assuntos().pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly assuntoId = computed(() => (this.assunto() ? Number(this.assunto()) : null));
  protected readonly soDisponiveis = computed(() => !!this.disponiveis());
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
        pagina: Math.max(0, Number(this.pagina() ?? 0) || 0),
        tamanho: TAMANHO,
      };
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
