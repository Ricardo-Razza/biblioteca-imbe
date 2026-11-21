import { Component, effect, inject, input, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { mensagemDeErro } from '../../core/api/erro-api';
import { ObraPublicaDetalhe } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { Selo } from '../../shared/selo/selo';
import { PublicoApi } from '../publico-api';
import { disponibilidade } from '../textos';

/** Ficha do livro com um único botão: Reservar (só com exemplar disponível). */
@Component({
  selector: 'app-livro',
  imports: [RouterLink, MatButtonModule, MatIconModule, CapaLivro, Selo],
  template: `
    <a class="voltar" routerLink="/"><mat-icon>arrow_back</mat-icon> Voltar ao catálogo</a>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (obra(); as o) {
      <article class="ficha">
        <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [titulo]="o.titulo" />
        <div class="dados">
          <h1>{{ o.titulo }}</h1>
          @if (o.subtitulo) {
            <p class="subtitulo">{{ o.subtitulo }}</p>
          }
          @if (o.autores.length) {
            <p class="autores">{{ o.autores.join(', ') }}</p>
          }
          <p class="texto-secundario">{{ detalhes(o) }}</p>

          <div class="reservar">
            <app-selo [tom]="o.disponiveis > 0 ? 'ok' : 'neutra'">{{ disponivel(o.disponiveis) }}</app-selo>
            @if (o.disponiveis > 0) {
              <a mat-flat-button class="botao" [routerLink]="['/reservar', o.id]"><mat-icon>bookmark_add</mat-icon> Reservar</a>
            } @else {
              <p class="texto-secundario">Todos os exemplares estão emprestados ou reservados. Volte a consultar nos próximos dias.</p>
            }
          </div>

          @if (o.sinopse) {
            <h2>Sinopse</h2>
            <p class="sinopse">{{ o.sinopse }}</p>
          }
          @if (o.assuntos.length) {
            <h2>Assuntos</h2>
            <ul class="assuntos">
              @for (a of o.assuntos; track a.id) {
                <li><a [routerLink]="['/']" [queryParams]="{ assunto: a.id }">{{ a.nome }}</a></li>
              }
            </ul>
          }
        </div>
      </article>
    }
  `,
  styles: `
    .voltar {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 16px;
      text-decoration: none;
    }
    .ficha {
      display: flex;
      gap: 32px;
      align-items: flex-start;
      --largura-capa: 240px;
    }
    .dados {
      flex: 1;
      min-width: 0;
      max-width: 680px;
    }
    .subtitulo {
      font: var(--mat-sys-title-medium);
      margin: 4px 0 0;
    }
    .autores {
      font: var(--mat-sys-title-small);
      color: var(--mat-sys-primary);
      margin: 8px 0 4px;
    }
    .reservar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px 16px;
      margin: 20px 0 28px;
      padding: 16px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container-low);
    }
    .reservar p {
      margin: 0;
    }
    .botao {
      height: 48px;
      min-width: 160px;
    }
    h2 {
      margin: 20px 0 8px;
    }
    .sinopse {
      white-space: pre-line;
      line-height: 1.6;
    }
    .assuntos {
      list-style: none;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .assuntos a {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 999px;
      border: 1px solid var(--mat-sys-outline-variant);
      text-decoration: none;
    }
    .assuntos a:hover {
      background: var(--mat-sys-surface-container);
    }
    @media (max-width: 640px) {
      .ficha {
        flex-direction: column;
        align-items: center;
        gap: 16px;
        --largura-capa: 180px;
      }
      .dados {
        width: 100%;
      }
    }
  `,
})
export class Livro {
  readonly id = input.required<string>();

  private readonly api = inject(PublicoApi);
  private readonly titulo = inject(Title);
  protected readonly obra = signal<ObraPublicaDetalhe | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly disponivel = disponibilidade;

  constructor() {
    effect((aoLimpar) => {
      this.obra.set(null);
      this.erro.set(null);
      const s = this.api.obra(Number(this.id())).subscribe({
        next: (o) => {
          this.obra.set(o);
          this.titulo.setTitle(`${o.titulo} · Sala Literária Castelinho`);
        },
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
      aoLimpar(() => s.unsubscribe());
    });
  }

  protected detalhes(o: ObraPublicaDetalhe): string {
    return [o.editora, o.ano, o.paginas ? `${o.paginas} páginas` : null].filter(Boolean).join(' · ');
  }
}
