import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { Sessao } from '../core/auth/sessao';

interface ItemMenu {
  rota: string;
  texto: string;
  icone: string;
  soAdmin?: boolean;
}

/** Layout da gestão: menu lateral com os 7 itens e Emprestar/Devolver sempre no topo. */
@Component({
  selector: 'app-layout-gestao',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <div class="gestao">
      <nav class="menu" aria-label="Menu da gestão">
        <div class="marca">
          <span class="nome">Biblioteca Municipal</span>
          <span class="cidade">Imbé</span>
        </div>
        @for (item of itensVisiveis(); track item.rota) {
          <a class="item" [routerLink]="item.rota" routerLinkActive="ativo">
            <mat-icon>{{ item.icone }}</mat-icon>
            {{ item.texto }}
          </a>
        }
      </nav>
      <div class="area">
        <header class="topo">
          <span class="espaco"></span>
          <a mat-flat-button routerLink="/painel/emprestar"><mat-icon>output</mat-icon> Emprestar</a>
          <a mat-stroked-button routerLink="/painel/devolver"><mat-icon>input</mat-icon> Devolver</a>
          @if (sessao.funcionario(); as f) {
            <button mat-button type="button" class="usuario" [matMenuTriggerFor]="menuUsuario" [attr.aria-label]="'Conta de ' + f.nome">
              <mat-icon>account_circle</mat-icon>
              <span class="nome-usuario">{{ primeiroNome() }}</span>
            </button>
            <mat-menu #menuUsuario="matMenu" xPosition="before">
              <div class="quem" mat-menu-item disabled>
                {{ f.nome }} · {{ f.perfil === 'ADMIN' ? 'Administrador' : 'Atendente' }}
              </div>
              @if (f.tipo === 'LOCAL') {
                <a mat-menu-item routerLink="/painel/senha"><mat-icon>key</mat-icon> Trocar senha</a>
              }
              <button mat-menu-item type="button" (click)="sair()"><mat-icon>logout</mat-icon> Sair</button>
            </mat-menu>
          }
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
    .usuario {
      margin-left: 8px;
    }
    @media (max-width: 760px) {
      .nome-usuario {
        display: none;
      }
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
  protected readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  protected readonly itens: ItemMenu[] = [
    { rota: 'inicio', texto: 'Início', icone: 'home' },
    { rota: 'acervo', texto: 'Acervo', icone: 'library_books' },
    { rota: 'leitores', texto: 'Leitores', icone: 'group' },
    { rota: 'emprestimos', texto: 'Empréstimos', icone: 'swap_horiz' },
    { rota: 'reservas', texto: 'Reservas', icone: 'bookmark' },
    { rota: 'relatorios', texto: 'Relatórios', icone: 'bar_chart' },
    { rota: 'configuracoes', texto: 'Configurações', icone: 'settings', soAdmin: true },
  ];
  protected readonly itensVisiveis = computed(() => this.itens.filter((i) => !i.soAdmin || this.sessao.admin()));
  protected readonly primeiroNome = computed(() => this.sessao.funcionario()?.nome.split(' ')[0] ?? '');

  protected sair(): void {
    this.sessao.sair();
    this.router.navigateByUrl('/login');
  }
}
