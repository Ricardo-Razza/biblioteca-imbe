import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';

import { Assinatura } from '../shared/assinatura/assinatura';
import { PublicoApi } from './publico-api';
import { RodapePublico } from './rodape-publico';

/** Site público, pensado primeiro para celular: marca e menu curto no topo; contato e como reservar no rodapé. */
@Component({
  selector: 'app-layout-publico',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Assinatura, RodapePublico],
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

    <app-rodape-publico [biblioteca]="biblioteca()" />
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
  `,
})
export class LayoutPublico {
  protected readonly biblioteca = toSignal(inject(PublicoApi).biblioteca().pipe(catchError(() => of(null))), {
    initialValue: null,
  });
}
