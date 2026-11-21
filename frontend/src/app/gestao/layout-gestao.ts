import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { Sessao } from '../core/auth/sessao';
import { Assinatura } from '../shared/assinatura/assinatura';

interface ItemMenu {
  rota: string;
  texto: string;
  icone: string;
  /** Título do grupo que começa neste item (só no menu lateral). */
  grupo?: string;
  soAdmin?: boolean;
}

/** Layout da gestão: menu lateral marinho com os 7 itens em grupos e Empréstimo/Devoluções sempre no topo. */
@Component({
  selector: 'app-layout-gestao',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule, Assinatura],
  template: `
    <div class="gestao">
      <nav class="menu" aria-label="Menu da gestão">
        <a class="marca" routerLink="inicio" aria-label="Sala Literária Castelinho: início da gestão">
          <app-assinatura [branca]="true" [tamanho]="12" />
        </a>
        @for (item of itensVisiveis(); track item.rota) {
          @if (item.grupo) {
            <span class="grupo" aria-hidden="true">{{ item.grupo }}</span>
          }
          <a class="item" [routerLink]="item.rota" routerLinkActive="ativo">
            <mat-icon>{{ item.icone }}</mat-icon>
            {{ item.texto }}
          </a>
        }
        <span class="faixa" aria-hidden="true"></span>
      </nav>
      <div class="area">
        <header class="topo">
          <span class="espaco"></span>
          <a mat-flat-button routerLink="/painel/emprestar"><mat-icon>output</mat-icon> Empréstimo</a>
          <a mat-stroked-button routerLink="/painel/devolver"><mat-icon>input</mat-icon> Devoluções</a>
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
    /* Menu marinho sólido, texto branco; o item ativo leva um filete amarelo à esquerda. */
    .menu {
      width: 264px;
      flex: none;
      position: sticky;
      top: 0;
      height: 100vh;
      overflow-y: auto;
      background: var(--marca-marinho);
      color: #fff;
      padding: 24px 12px 0;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .marca {
      display: flex;
      padding: 0 8px 20px;
      text-decoration: none;
      border-radius: 4px;
    }
    .marca:focus-visible,
    .item:focus-visible {
      outline: 2px solid #fff;
      outline-offset: -2px;
    }
    .grupo {
      padding: 18px 12px 6px;
      font: var(--mat-sys-label-small);
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: rgb(255 255 255 / 0.55);
    }
    .item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 9px 12px;
      border-left: 3px solid transparent;
      color: rgb(255 255 255 / 0.8);
      text-decoration: none;
      font: var(--mat-sys-body-medium);
      font-size: 15px;
    }
    .item mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
      font-weight: 300;
    }
    .item:hover {
      color: #fff;
      background: rgb(255 255 255 / 0.06);
    }
    .item.ativo {
      color: #fff;
      background: rgb(255 255 255 / 0.1);
      border-left-color: var(--prefeitura-amarelo);
      font-weight: 500;
    }
    /* Faixa de cinco cores da Prefeitura no pé do menu (a do marinho mais escura, para aparecer no fundo marinho). */
    .faixa {
      margin: auto -12px 0;
      flex: none;
      height: 6px;
      background: linear-gradient(
        90deg,
        color-mix(in srgb, var(--marca-marinho) 70%, #000) 0 20%,
        var(--prefeitura-azul) 20% 40%,
        var(--prefeitura-verde) 40% 60%,
        var(--prefeitura-amarelo) 60% 80%,
        var(--prefeitura-vermelho) 80% 100%
      );
    }
    .area {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      background: var(--papel);
    }
    .topo {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
      padding: 12px 32px;
      background: var(--mat-sys-surface);
      border-bottom: 1px solid var(--filete);
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
      .topo {
        padding: 8px 16px;
      }
      .usuario {
        margin-left: 0;
      }
    }
    .conteudo {
      padding: 28px 32px 40px;
      max-width: 1240px;
    }
    @media (max-width: 760px) {
      .gestao {
        flex-direction: column;
      }
      .menu {
        position: static;
        width: auto;
        height: auto;
        flex-direction: row;
        overflow-x: auto;
        padding: 0 8px;
      }
      .marca,
      .grupo,
      .faixa {
        display: none;
      }
      .item {
        flex: none;
        border-left: 0;
        border-bottom: 3px solid transparent;
        padding: 14px 10px 11px;
        white-space: nowrap;
        gap: 6px;
      }
      .item.ativo {
        background: none;
        border-bottom-color: var(--prefeitura-amarelo);
      }
      .conteudo {
        padding: 20px 16px 32px;
      }
    }
  `,
})
export class LayoutGestao {
  protected readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  protected readonly itens: ItemMenu[] = [
    { rota: 'inicio', texto: 'Início', icone: 'home' },
    { rota: 'emprestimos', texto: 'Empréstimos', icone: 'swap_horiz', grupo: 'Balcão' },
    { rota: 'reservas', texto: 'Reservas', icone: 'bookmark' },
    { rota: 'leitores', texto: 'Leitores', icone: 'group' },
    { rota: 'acervo', texto: 'Acervo', icone: 'library_books', grupo: 'Acervo' },
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
