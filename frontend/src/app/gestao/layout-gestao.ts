import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

interface ItemMenu {
  rota: string;
  texto: string;
  icone: string;
}

/** Layout da gestão: menu lateral com os 7 itens e Emprestar/Devolver sempre no topo. */
@Component({
  selector: 'app-layout-gestao',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="gestao">
      <nav class="menu" aria-label="Menu da gestão">
        <div class="marca">
          <span class="nome">Biblioteca Municipal</span>
          <span class="cidade">Imbé</span>
        </div>
        @for (item of itens; track item.rota) {
          <a class="item" [routerLink]="item.rota" routerLinkActive="ativo">
            <mat-icon>{{ item.icone }}</mat-icon>
            {{ item.texto }}
          </a>
        }
      </nav>
      <div class="area">
        <header class="topo">
          <span class="espaco"></span>
          <!-- Os fluxos de empréstimo chegam na fase 2 do roadmap. -->
          <span matTooltip="Chega na fase 2">
            <button mat-flat-button disabled><mat-icon>output</mat-icon> Emprestar</button>
          </span>
          <span matTooltip="Chega na fase 2">
            <button mat-stroked-button disabled><mat-icon>input</mat-icon> Devolver</button>
          </span>
        </header>
        <main class="conteudo">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: `
    .gestao {
      display: flex;
      min-height: 100vh;
    }
    .menu {
      width: 232px;
      flex: none;
      background: var(--mat-sys-surface-container-low);
      border-right: 1px solid var(--mat-sys-outline-variant);
      padding: 16px 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .marca {
      display: flex;
      flex-direction: column;
      padding: 4px 12px 20px;
    }
    .nome {
      font: var(--mat-sys-title-medium);
      color: var(--mat-sys-primary);
    }
    .cidade {
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 8px;
      color: var(--mat-sys-on-surface);
      text-decoration: none;
    }
    .item:hover {
      background: var(--mat-sys-surface-container-highest);
    }
    .item.ativo {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
      font-weight: 500;
    }
    .area {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .topo {
      display: flex;
      gap: 8px;
      align-items: center;
      padding: 12px 24px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .espaco {
      flex: 1;
    }
    .conteudo {
      padding: 24px;
      max-width: 1100px;
    }
    @media (max-width: 760px) {
      .gestao {
        flex-direction: column;
      }
      .menu {
        width: auto;
        flex-direction: row;
        overflow-x: auto;
        border-right: none;
        border-bottom: 1px solid var(--mat-sys-outline-variant);
      }
      .marca {
        display: none;
      }
      .conteudo {
        padding: 16px;
      }
    }
  `,
})
export class LayoutGestao {
  protected readonly itens: ItemMenu[] = [
    { rota: 'inicio', texto: 'Início', icone: 'home' },
    { rota: 'acervo', texto: 'Acervo', icone: 'library_books' },
    { rota: 'leitores', texto: 'Leitores', icone: 'group' },
    { rota: 'emprestimos', texto: 'Empréstimos', icone: 'swap_horiz' },
    { rota: 'reservas', texto: 'Reservas', icone: 'bookmark' },
    { rota: 'relatorios', texto: 'Relatórios', icone: 'bar_chart' },
    { rota: 'configuracoes', texto: 'Configurações', icone: 'settings' },
  ];
}
