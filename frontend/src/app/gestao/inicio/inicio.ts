import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Painel } from '../../core/api/modelos';
import { Sessao } from '../../core/auth/sessao';
import { dataBr, dias } from '../../shared/formatos';
import { Selo } from '../../shared/selo/selo';
import { dataComDia, protocoloLegivel } from '../../publico/textos';
import { MovimentoSemana } from './movimento-semana';

/** Tela inicial da gestão: o que precisa de atenção hoje e o movimento da semana. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MovimentoSemana, Selo],
  template: `
    <div class="cabecalho">
      <div>
        <h1>{{ saudacao() }}</h1>
        @if (painel(); as p) {
          <p class="texto-secundario">{{ hojeExtenso(p.hoje) }}</p>
        }
      </div>
      <button mat-icon-button type="button" (click)="carregar()" [disabled]="carregando()" aria-label="Atualizar">
        <mat-icon>refresh</mat-icon>
      </button>
    </div>

    @if (carregando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    @if (painel(); as p) {
      <div class="cartoes">
        <a class="cartao azul" routerLink="/painel/emprestimos">
          <mat-icon class="icone" aria-hidden="true">menu_book</mat-icon>
          <span class="numero">{{ p.contadores.emprestados }}</span>
          <span>Empréstimos ativos</span>
        </a>
        <div class="cartao amarelo" [class.destaque]="p.contadores.venceHoje > 0">
          <mat-icon class="icone" aria-hidden="true">event</mat-icon>
          <span class="numero">{{ p.contadores.venceHoje }}</span>
          <span>Vencimentos ativos</span>
        </div>
        <a class="cartao vermelho" [class.destaque]="p.contadores.atrasados > 0" routerLink="/painel/emprestimos" [queryParams]="{ status: 'ATRASADO' }">
          <mat-icon class="icone" aria-hidden="true">schedule</mat-icon>
          <span class="numero">{{ p.contadores.atrasados }}</span>
          <span>Em atraso</span>
        </a>
        <a class="cartao verde" [class.destaque]="p.contadores.reservasParaSeparar > 0" routerLink="/painel/reservas">
          <mat-icon class="icone" aria-hidden="true">inventory_2</mat-icon>
          <span class="numero">{{ p.contadores.reservasParaSeparar }}</span>
          <span>Exemplares separados</span>
        </a>
        <div class="cartao marinho">
          <mat-icon class="icone" aria-hidden="true">person_off</mat-icon>
          <span class="numero">{{ p.contadores.leitoresBloqueados }}</span>
          <span>Leitores bloqueados</span>
        </div>
      </div>

      <div class="colunas">
        <section>
          <h2>Exemplares separados</h2>
          @if (p.paraSeparar.length === 0) {
            <p class="texto-secundario">Nenhuma reserva esperando.</p>
          } @else {
            <ul class="lista">
              @for (r of p.paraSeparar; track r.id) {
                <li>
                  <div class="info">
                    <a class="titulo" [routerLink]="['/painel/acervo', r.obraId]">{{ r.titulo }}</a>
                    <span><strong>Tombo {{ r.tombo }}</strong>{{ r.localizacao ? ' · ' + r.localizacao : '' }}</span>
                    <span class="texto-secundario">
                      <a [routerLink]="['/painel/leitores', r.leitorId]">{{ r.leitor }}</a> · {{ legivel(r.protocolo) }}
                    </span>
                  </div>
                  <app-selo [tom]="r.prazoRetirada <= p.hoje ? 'alerta' : 'ok'">
                    {{ r.prazoRetirada <= p.hoje ? 'Último dia hoje' : 'Até ' + data(r.prazoRetirada) }}
                  </app-selo>
                </li>
              }
            </ul>
            @if (p.contadores.reservasParaSeparar > p.paraSeparar.length) {
              <a mat-button routerLink="/painel/reservas">Ver todas ({{ p.contadores.reservasParaSeparar }})</a>
            }
          }
        </section>

        <section>
          <h2>Vencimentos ativos</h2>
          @if (p.venceHoje.length === 0) {
            <p class="texto-secundario">Nenhuma devolução prevista para hoje.</p>
          } @else {
            <ul class="lista">
              @for (e of p.venceHoje; track e.id) {
                <li>
                  <div class="info">
                    <a class="titulo" [routerLink]="['/painel/acervo', e.obraId]">{{ e.titulo }}</a>
                    <span class="texto-secundario">
                      Tombo {{ e.tombo }} · <a [routerLink]="['/painel/leitores', e.leitorId]">{{ e.leitor }}</a>
                    </span>
                  </div>
                </li>
              }
            </ul>
            @if (p.contadores.venceHoje > p.venceHoje.length) {
              <p class="texto-secundario">E mais {{ p.contadores.venceHoje - p.venceHoje.length }}.</p>
            }
          }
        </section>

        <section>
          <h2>Em atraso</h2>
          @if (p.atrasados.length === 0) {
            <p class="texto-secundario">Nenhum empréstimo atrasado.</p>
          } @else {
            <ul class="lista">
              @for (e of p.atrasados; track e.id) {
                <li>
                  <div class="info">
                    <a class="titulo" [routerLink]="['/painel/acervo', e.obraId]">{{ e.titulo }}</a>
                    <span class="texto-secundario">
                      Tombo {{ e.tombo }} · <a [routerLink]="['/painel/leitores', e.leitorId]">{{ e.leitor }}</a>
                      {{ e.telefone ? ' · ' + e.telefone : '' }}
                    </span>
                  </div>
                  <app-selo tom="erro">{{ dias(e.diasAtraso) }}</app-selo>
                </li>
              }
            </ul>
            <a mat-button routerLink="/painel/emprestimos" [queryParams]="{ status: 'ATRASADO' }">
              Ver {{ p.contadores.atrasados > p.atrasados.length ? 'todos (' + p.contadores.atrasados + ')' : 'na lista' }}
            </a>
          }
        </section>

        <section>
          <h2>Quadro de movimentações</h2>
          <app-movimento-semana [dias]="p.movimento" />
        </section>
      </div>
    }
  `,
  styles: `
    .cabecalho {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 16px;
    }
    .cabecalho p {
      margin: 4px 0 0;
    }
    .cartoes {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(170px, 100%), 1fr));
      gap: 12px;
      margin: 8px 0 24px;
    }
    /* Cada indicador tem a sua cor da paleta da Prefeitura, para identificar de relance. */
    .cartao {
      --cor: var(--marca-marinho);
      --tinta: 8%;
      position: relative;
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
      padding: 16px 16px 14px;
      border-radius: 12px;
      border: 1px solid color-mix(in srgb, var(--cor) 30%, var(--mat-sys-outline-variant));
      border-top: 4px solid var(--cor);
      background: color-mix(in srgb, var(--cor) var(--tinta), var(--mat-sys-surface));
      box-shadow: var(--sombra-leve);
      color: var(--mat-sys-on-surface-variant);
      text-decoration: none;
      transition: box-shadow 0.15s, transform 0.15s;
    }
    .cartao.azul { --cor: var(--prefeitura-azul); }
    .cartao.amarelo { --cor: var(--prefeitura-amarelo); }
    .cartao.vermelho { --cor: var(--prefeitura-vermelho); }
    .cartao.verde { --cor: var(--prefeitura-verde); }
    .cartao.marinho { --cor: var(--marca-marinho); }
    /* Número que pede atenção: fundo mais forte. */
    .cartao.destaque {
      --tinta: 18%;
    }
    a.cartao:hover {
      box-shadow: var(--sombra-media);
      transform: translateY(-2px);
    }
    .cartao .icone {
      position: absolute;
      top: 12px;
      right: 12px;
      color: var(--cor);
    }
    .cartao .numero {
      font: var(--mat-sys-headline-medium);
      color: var(--mat-sys-on-surface);
    }
    .colunas {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(340px, 100%), 1fr));
      gap: 24px 32px;
    }
    section {
      min-width: 0;
    }
    h2 {
      margin: 0 0 8px;
    }
    .lista {
      list-style: none;
      padding: 0;
      margin: 0 0 4px;
    }
    .lista li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      padding: 8px 0;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .info {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .titulo {
      font-weight: 500;
      color: inherit;
    }
    .info a {
      text-decoration: none;
    }
    .info a:hover {
      text-decoration: underline;
    }
    @media (max-width: 420px) {
      .colunas {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class Inicio {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(Sessao);
  protected readonly painel = signal<Painel | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly data = dataBr;
  protected readonly dias = dias;
  protected readonly legivel = protocoloLegivel;

  constructor() {
    this.carregar();
  }

  protected saudacao(): string {
    const h = new Date().getHours();
    const parte = h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
    const nome = this.sessao.funcionario()?.nome?.split(' ')[0];
    return nome ? `${parte}, ${nome}` : parte;
  }

  protected hojeExtenso(iso: string): string {
    const texto = dataComDia(iso);
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.http.get<Painel>('/api/v1/painel').subscribe({
      next: (p) => {
        this.carregando.set(false);
        this.painel.set(p);
      },
      error: (e) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}
