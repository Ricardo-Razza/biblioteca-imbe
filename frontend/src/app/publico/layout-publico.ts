import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of } from 'rxjs';

import { Assinatura } from '../shared/assinatura/assinatura';
import { PublicoApi } from './publico-api';

/** Site público, pensado primeiro para celular: marca e menu curto no topo; endereço e horário no rodapé. */
@Component({
  selector: 'app-layout-publico',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, Assinatura],
  template: `
    <header class="topo">
      <div class="faixa">
        <a class="marca" routerLink="/" aria-label="Biblioteca Municipal de Imbé: início do catálogo">
          <app-assinatura />
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
        <div class="prefeitura">
          <img src="marca/imbe-horizontal-branca.png" alt="Prefeitura de Imbé: um lugar cheio de vida" height="64" />
          <span class="links">
            <a href="https://www.imbe.rs.gov.br" rel="noopener" target="_blank">www.imbe.rs.gov.br</a>
            <a routerLink="/login">Área da equipe</a>
          </span>
        </div>
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
      background: #fff;
      border-top: 6px solid transparent;
      border-image: var(--marca-faixa) 1;
      box-shadow: inset 0 -1px 0 var(--mat-sys-outline-variant);
    }
    .topo .faixa {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px 24px;
      padding-top: 14px;
      padding-bottom: 14px;
    }
    .marca {
      display: flex;
      text-decoration: none;
      border-radius: 8px;
    }
    nav {
      display: flex;
      gap: 4px;
    }
    nav a {
      color: var(--marca-azul);
      text-decoration: none;
      padding: 10px 14px;
      border-radius: 8px;
      font: var(--mat-sys-label-large);
      font-weight: 600;
    }
    nav a:hover {
      background: color-mix(in srgb, var(--marca-azul) 8%, transparent);
    }
    nav a.ativo {
      background: color-mix(in srgb, var(--marca-azul) 14%, transparent);
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
      background: var(--marca-marinho);
      color: #dfe6f0;
      font: var(--mat-sys-body-medium);
    }
    .rodape .faixa {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 24px;
      padding-top: 28px;
      padding-bottom: 28px;
    }
    .unidade {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .unidade strong {
      color: #fff;
      font-family: var(--fonte-marca);
      font-weight: 500;
    }
    .prefeitura {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 12px;
    }
    .prefeitura img {
      display: block;
      height: 64px;
      width: auto;
    }
    .links {
      display: flex;
      gap: 16px;
    }
    .links a {
      color: #fff;
    }
    @media (max-width: 600px) {
      .prefeitura {
        align-items: flex-start;
      }
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
  `,
})
export class LayoutPublico {
  protected readonly biblioteca = toSignal(inject(PublicoApi).biblioteca().pipe(catchError(() => of(null))), {
    initialValue: null,
  });
}
