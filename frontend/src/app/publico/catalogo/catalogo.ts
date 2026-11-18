import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
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

/** Catálogo: busca grande no topo, filtros e grade de capas. Tudo na URL (?q=&assunto=&disponiveis=&pagina=). */
@Component({
  selector: 'app-catalogo',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    CapaLivro,
    Selo,
  ],
  template: `
    <section class="busca" aria-labelledby="titulo-busca">
      <h1 id="titulo-busca">Encontre um livro</h1>
      <form class="linha-busca" role="search" (ngSubmit)="buscar()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="campo">
          <mat-icon matPrefix>search</mat-icon>
          <input matInput name="q" [(ngModel)]="texto" placeholder="Título, autor ou assunto" autocomplete="off" aria-label="Buscar por título, autor ou assunto" />
          @if (texto) {
            <button matSuffix mat-icon-button type="button" (click)="limpar()" aria-label="Limpar a busca"><mat-icon>close</mat-icon></button>
          }
        </mat-form-field>
        <button mat-flat-button type="submit" class="botao-buscar">Buscar</button>
      </form>
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
          Só os disponíveis agora
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
    .busca {
      margin-bottom: 16px;
    }
    h1 {
      margin-bottom: 12px;
    }
    .linha-busca {
      display: flex;
      gap: 8px;
      align-items: center;
      max-width: 720px;
    }
    .campo {
      flex: 1;
      font-size: 18px;
    }
    .botao-buscar {
      height: 56px;
    }
    .filtros {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px 24px;
      margin-top: 12px;
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
      padding: 8px;
      border-radius: 8px;
      color: inherit;
      text-decoration: none;
      --largura-capa: 100%;
    }
    .cartao:hover {
      background: var(--mat-sys-surface-container);
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
        gap: 12px 8px;
      }
      .botao-buscar {
        display: none;
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

  protected texto = '';
  protected readonly assuntos = toSignal(this.api.assuntos().pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly assuntoId = computed(() => (this.assunto() ? Number(this.assunto()) : null));
  protected readonly soDisponiveis = computed(() => !!this.disponiveis());
  protected readonly resultado = signal<Pagina<ObraPublica> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly disponivel = disponibilidade;

  constructor() {
    effect(() => {
      this.texto = this.q() ?? '';
    });
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

  protected buscar(): void {
    this.navegar({ q: this.texto.trim() || null, pagina: null });
  }

  protected limpar(): void {
    this.texto = '';
    this.navegar({ q: null, pagina: null });
  }

  protected irPara(pagina: number): void {
    this.navegar({ pagina: pagina || null });
    window.scrollTo({ top: 0 });
  }

  protected navegar(params: Record<string, string | number | null>): void {
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }
}
