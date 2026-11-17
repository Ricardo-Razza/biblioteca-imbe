import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../core/api/erro-api';
import { PaginadorPtBr } from '../../core/paginador-pt-br';
import { Obra, Pagina } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { Sessao } from '../../core/auth/sessao';
import { AcervoApi } from './acervo-api';

const TAMANHO = 20;

/** Acervo: busca por ISBN, título ou autor. Termo e página ficam na URL para o "voltar" do navegador. */
@Component({
  selector: 'app-lista-acervo',
  imports: [
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    CapaLivro,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <div class="cabecalho">
      <h1>Acervo</h1>
      <div class="acoes-cabecalho">
        @if (sessao.admin()) {
          <a mat-stroked-button routerLink="importar"><mat-icon>upload_file</mat-icon> Importar planilha</a>
        }
        <a mat-flat-button routerLink="novo"><mat-icon>add</mat-icon> Cadastrar livro</a>
      </div>
    </div>

    <mat-form-field appearance="outline" class="busca" subscriptSizing="dynamic">
      <mat-label>Buscar</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [value]="q() ?? ''" (input)="digitar($any($event.target).value)" autocomplete="off" />
      <mat-hint>ISBN, trecho do título ou nome do autor.</mat-hint>
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
          @if (q()) {
            Nada encontrado para "{{ q() }}".
          } @else {
            O acervo ainda está vazio. Comece em "Cadastrar livro".
          }
        </p>
      } @else {
        <ul class="lista">
          @for (o of r.itens; track o.id) {
            <li>
              <a class="obra" [routerLink]="[o.id]">
                <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [mini]="true" [titulo]="o.titulo" />
                <span class="info">
                  <span class="titulo">{{ o.titulo }}</span>
                  @if (o.autores.length) {
                    <span>{{ o.autores.join(', ') }}</span>
                  }
                  <span class="texto-secundario">ISBN {{ isbn(o) }}</span>
                </span>
                <span class="disponiveis" [class.nenhum]="o.exemplaresDisponiveis === 0">
                  {{ textoDisponiveis(o) }}
                </span>
              </a>
            </li>
          }
        </ul>
        <mat-paginator
          [length]="r.total"
          [pageIndex]="r.pagina"
          [pageSize]="tamanho"
          [hidePageSize]="true"
          (page)="mudarPagina($event)"
        />
      }
    }
  `,
  styles: `
    .acoes-cabecalho {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
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
    .obra {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 10px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      color: inherit;
      text-decoration: none;
      --largura-capa: 48px;
    }
    .obra:hover {
      background: var(--mat-sys-surface-container);
    }
    .info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .titulo {
      font-weight: 500;
    }
    .disponiveis {
      white-space: nowrap;
      color: var(--situacao-ok);
    }
    .disponiveis.nenhum {
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class ListaAcervo {
  /** Query params (?q=&pagina=), ligados pelo router. */
  readonly q = input<string>();
  readonly pagina = input<string>();

  protected readonly sessao = inject(Sessao);
  private readonly api = inject(AcervoApi);
  private readonly router = inject(Router);

  protected readonly tamanho = TAMANHO;
  protected readonly resultado = signal<Pagina<Obra> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect((aoLimpar) => {
      const termo = this.q() ?? '';
      const pagina = Math.max(0, Number(this.pagina() ?? 0) || 0);
      this.carregando.set(true);
      this.erro.set(null);
      const s = this.api.listarObras(termo, pagina, TAMANHO).subscribe({
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

  protected isbn(o: Obra): string {
    return o.isbn13;
  }

  protected textoDisponiveis(o: Obra): string {
    if (o.totalExemplares === 0) return 'Sem exemplares';
    if (o.exemplaresDisponiveis === 0) return 'Nenhum disponível';
    return o.exemplaresDisponiveis === 1 ? '1 disponível' : `${o.exemplaresDisponiveis} disponíveis`;
  }

  private navegar(q: string, pagina: number): void {
    this.router.navigate([], {
      queryParams: { q: q.trim() ? q : null, pagina: pagina || null },
      replaceUrl: true,
    });
  }
}
