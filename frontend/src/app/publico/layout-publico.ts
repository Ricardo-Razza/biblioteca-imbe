import { Component, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { catchError, filter, of } from 'rxjs';

import { Assinatura } from '../shared/assinatura/assinatura';
import { PublicoApi } from './publico-api';
import { RodapePublico } from './rodape-publico';

/**
 * Site público, pensado primeiro para celular. Cabeçalho azul da Prefeitura com a foto das estantes ao fundo,
 * a marca branca e a busca ("Encontre um livro"), que vale em todas as páginas e leva ao catálogo.
 * Logo abaixo, as abas Catálogo e Minhas reservas; contato e como reservar no rodapé.
 */
@Component({
  selector: 'app-layout-publico',
  imports: [FormsModule, RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, Assinatura, RodapePublico],
  template: `
    <header class="topo">
      <div class="faixa">
        <a class="marca" routerLink="/" aria-label="Sala Literária Castelinho: início do catálogo">
          <app-assinatura [branca]="true" />
        </a>
        <form class="busca" role="search" (ngSubmit)="buscar()">
          <label class="chamada" for="busca-livro">Encontre um livro</label>
          <div class="barra">
            <mat-icon class="lupa" aria-hidden="true">search</mat-icon>
            <input
              id="busca-livro"
              name="q"
              type="search"
              [(ngModel)]="texto"
              placeholder="Título, autor ou assunto"
              autocomplete="off"
              enterkeyhint="search"
            />
            @if (texto) {
              <button type="button" class="limpar" (click)="limpar()" aria-label="Limpar a busca">
                <mat-icon>close</mat-icon>
              </button>
            }
            <button type="submit" class="buscar" aria-label="Buscar">
              <mat-icon class="so-icone" aria-hidden="true">search</mat-icon>
              <span class="rotulo">Buscar</span>
            </button>
          </div>
        </form>
      </div>
    </header>

    <nav class="abas" aria-label="Site da biblioteca">
      <div class="faixa">
        <a routerLink="/" routerLinkActive="ativo" [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }">
          <mat-icon aria-hidden="true">menu_book</mat-icon> Catálogo
        </a>
        <a routerLink="/minhas-reservas" routerLinkActive="ativo">
          <mat-icon aria-hidden="true">bookmark</mat-icon> Minhas reservas
        </a>
      </div>
    </nav>

    <main class="conteudo">
      <router-outlet />
    </main>

    <app-rodape-publico [biblioteca]="biblioteca()" />
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
      background: var(--mat-sys-surface-container-low);
    }
    .faixa {
      max-width: 1120px;
      margin: 0 auto;
      padding: 0 16px;
    }
    /* Foto das estantes ao fundo, sob um véu no azul da Prefeitura: a marca branca e a busca continuam legíveis. */
    .topo {
      position: relative;
      z-index: 1;
      isolation: isolate;
      background: var(--prefeitura-azul) url('/imagens/cabecalho-livros.jpg') center 40% / cover no-repeat;
      border-top: 6px solid transparent;
      border-image: var(--prefeitura-faixa) 1;
      color: #fff;
    }
    /* Pé do cabeçalho em degradê: o azul se dissolve no branco das abas, sem corte seco. */
    .topo::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 88px;
      z-index: -1;
      background: linear-gradient(
        180deg,
        transparent 0%,
        color-mix(in srgb, var(--mat-sys-surface) 35%, transparent) 45%,
        color-mix(in srgb, var(--mat-sys-surface) 80%, transparent) 75%,
        var(--mat-sys-surface) 100%
      );
    }
    .topo::before {
      content: '';
      position: absolute;
      inset: 0;
      z-index: -1;
      background: linear-gradient(
        90deg,
        color-mix(in srgb, var(--prefeitura-azul) 94%, transparent) 0%,
        color-mix(in srgb, var(--prefeitura-azul) 86%, transparent) 55%,
        color-mix(in srgb, var(--prefeitura-azul) 78%, transparent) 100%
      );
    }
    .topo .faixa {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 24px 40px;
      padding-top: 32px;
      /* espaço para o degradê do pé, abaixo da busca */
      padding-bottom: 96px;
    }
    .marca {
      display: flex;
      min-width: 0;
      text-decoration: none;
      border-radius: 8px;
    }
    .marca:focus-visible,
    .barra button:focus-visible {
      outline: 2px solid #fff;
      outline-offset: 3px;
    }
    .busca {
      flex: 1 1 360px;
      min-width: 0;
      max-width: 620px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .chamada {
      font-family: var(--fonte-marca);
      font-size: clamp(20px, 4.2vw, 28px);
      font-weight: 600;
      line-height: 1.2;
    }
    /* Barra arredondada com o botão dentro; a borda tem a cor do botão. */
    .barra {
      --cor-botao: var(--marca-marinho);
      display: flex;
      align-items: center;
      min-width: 0;
      height: 56px;
      padding: 0 4px 0 16px;
      background: #fff;
      border: 2px solid var(--cor-botao);
      border-radius: 999px;
      color: var(--mat-sys-on-surface);
      box-shadow: 0 2px 8px rgb(0 0 0 / 0.18);
    }
    .barra:focus-within {
      box-shadow:
        0 0 0 3px color-mix(in srgb, #fff 70%, transparent),
        0 2px 8px rgb(0 0 0 / 0.18);
    }
    .lupa {
      flex: none;
      color: var(--mat-sys-on-surface-variant);
    }
    input {
      flex: 1 1 auto;
      min-width: 0;
      height: 100%;
      padding: 0 8px 0 10px;
      border: 0;
      outline: 0;
      background: transparent;
      color: inherit;
      font: var(--mat-sys-body-large);
      font-size: 17px;
    }
    input::-webkit-search-cancel-button {
      display: none;
    }
    .barra button {
      flex: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border: 0;
      cursor: pointer;
      font: var(--mat-sys-label-large);
    }
    .limpar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: transparent;
      color: var(--mat-sys-on-surface-variant);
    }
    .limpar:hover {
      background: var(--mat-sys-surface-container-high);
    }
    .buscar {
      gap: 6px;
      height: 44px;
      padding: 0 22px;
      border-radius: 999px;
      background: var(--cor-botao);
      color: #fff;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
    .buscar:hover {
      background: color-mix(in srgb, var(--cor-botao) 85%, #fff);
    }
    .so-icone {
      display: none;
    }
    /* Abas logo abaixo do cabeçalho */
    .abas {
      background: var(--mat-sys-surface);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .abas .faixa {
      display: flex;
      flex-wrap: wrap;
      gap: 4px 8px;
    }
    .abas a {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 14px 12px 12px;
      border-bottom: 3px solid transparent;
      color: var(--mat-sys-on-surface-variant);
      text-decoration: none;
      font: var(--mat-sys-label-large);
      font-weight: 600;
    }
    .abas mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    .abas a:hover {
      color: var(--prefeitura-azul);
    }
    .abas a.ativo {
      color: var(--prefeitura-azul);
      border-bottom-color: var(--prefeitura-azul);
    }
    .conteudo {
      flex: 1;
      width: 100%;
      max-width: 1120px;
      margin: 0 auto;
      padding: 28px 16px 48px;
    }
    @media (max-width: 480px) {
      .topo .faixa {
        padding-top: 24px;
        padding-bottom: 80px;
      }
      .barra {
        height: 52px;
        padding-left: 12px;
      }
      .lupa {
        display: none;
      }
      .buscar {
        width: 44px;
        padding: 0;
      }
      .rotulo {
        display: none;
      }
      .so-icone {
        display: inline-block;
      }
    }
    /* Celular estreito: as duas abas numa linha só */
    @media (max-width: 380px) {
      .abas .faixa {
        gap: 0;
        padding: 0 8px;
      }
      .abas a {
        padding: 14px 8px 12px;
      }
      .abas mat-icon {
        display: none;
      }
    }
  `,
})
export class LayoutPublico {
  private readonly router = inject(Router);

  protected texto = '';
  protected readonly biblioteca = toSignal(inject(PublicoApi).biblioteca().pipe(catchError(() => of(null))), {
    initialValue: null,
  });

  constructor() {
    // A barra mostra a busca que está na URL do catálogo (e fica vazia nas outras páginas).
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      this.texto = this.noCatalogo() ? (this.router.parseUrl(this.router.url).queryParams['q'] ?? '') : '';
    });
  }

  /** Buscar de qualquer página leva ao catálogo; no catálogo, mantém os filtros e volta à primeira página. */
  protected buscar(): void {
    const q = this.texto.trim() || null;
    if (this.noCatalogo()) {
      this.router.navigate(['/'], { queryParams: { q, pagina: null }, queryParamsHandling: 'merge' });
    } else {
      this.router.navigate(['/'], { queryParams: { q } });
    }
  }

  protected limpar(): void {
    this.texto = '';
    if (this.noCatalogo()) this.buscar();
  }

  private noCatalogo(): boolean {
    return this.router.parseUrl(this.router.url).root.children['primary'] === undefined;
  }
}
