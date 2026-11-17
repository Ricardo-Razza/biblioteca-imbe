import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of } from 'rxjs';

import { PublicoApi } from './publico-api';

/** Site público, pensado primeiro para celular: marca e menu curto no topo; endereço e horário no rodapé. */
@Component({
  selector: 'app-layout-publico',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule],
  template: `
    <header class="topo">
      <div class="faixa">
        <a class="marca" routerLink="/" aria-label="Início do catálogo">
          <mat-icon class="logo" aria-hidden="true">local_library</mat-icon>
          <span class="nomes">
            <span class="nome">Biblioteca Municipal</span>
            <span class="cidade">Prefeitura de Imbé</span>
          </span>
        </a>
        <nav aria-label="Site da biblioteca">
          <a routerLink="/" routerLinkActive="ativo" [routerLinkActiveOptions]="{ exact: true }">Catálogo</a>
          <a routerLink="/minhas-reservas" routerLinkActive="ativo">Minhas reservas</a>
        </nav>
      </div>
    </header>

    <main class="conteudo">
      <router-outlet />
    </main>

    <footer class="rodape">
      <div class="faixa">
        @if (biblioteca(); as b) {
          <div class="unidade">
            <strong>{{ b.nome }}</strong>
            @if (b.endereco) {
              <span><mat-icon aria-hidden="true">location_on</mat-icon> {{ b.endereco }}</span>
            }
            @if (b.horario) {
              <span><mat-icon aria-hidden="true">schedule</mat-icon> {{ b.horario }}</span>
            }
            @if (b.telefone) {
              <span><mat-icon aria-hidden="true">call</mat-icon> {{ b.telefone }}</span>
            }
          </div>
        }
        <a class="equipe" routerLink="/login">Área da equipe</a>
      </div>
    </footer>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    .faixa {
      max-width: 1120px;
      margin: 0 auto;
      padding: 0 16px;
    }
    .topo {
      background: var(--mat-sys-primary);
      color: var(--mat-sys-on-primary);
    }
    .topo .faixa {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px 24px;
      padding-top: 12px;
      padding-bottom: 12px;
    }
    .marca {
      display: flex;
      align-items: center;
      gap: 10px;
      color: inherit;
      text-decoration: none;
    }
    .logo {
      font-size: 36px;
      width: 36px;
      height: 36px;
    }
    .nomes {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }
    .nome {
      font: var(--mat-sys-title-medium);
    }
    .cidade {
      font: var(--mat-sys-body-small);
      opacity: 0.85;
    }
    nav {
      display: flex;
      gap: 4px;
    }
    nav a {
      color: inherit;
      text-decoration: none;
      padding: 8px 12px;
      border-radius: 8px;
      font: var(--mat-sys-label-large);
    }
    nav a:hover {
      background: color-mix(in srgb, var(--mat-sys-on-primary) 12%, transparent);
    }
    nav a.ativo {
      background: color-mix(in srgb, var(--mat-sys-on-primary) 18%, transparent);
    }
    .conteudo {
      flex: 1;
      width: 100%;
      max-width: 1120px;
      margin: 0 auto;
      padding: 24px 16px 40px;
      box-sizing: border-box;
    }
    .rodape {
      background: var(--mat-sys-surface-container);
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
    .rodape .faixa {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: flex-end;
      gap: 16px;
      padding-top: 20px;
      padding-bottom: 20px;
    }
    .unidade {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .unidade strong {
      color: var(--mat-sys-on-surface);
    }
    .unidade span {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .unidade mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
    .equipe {
      color: inherit;
    }
  `,
})
export class LayoutPublico {
  protected readonly biblioteca = toSignal(inject(PublicoApi).biblioteca().pipe(catchError(() => of(null))), {
    initialValue: null,
  });
}
