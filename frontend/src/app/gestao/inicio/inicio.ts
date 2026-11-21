import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Painel } from '../../core/api/modelos';
import { Sessao } from '../../core/auth/sessao';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { dataBr, dias } from '../../shared/formatos';
import { dataComDia, protocoloLegivel } from '../../publico/textos';
import { diaDaSemana, MovimentoSemana } from './movimento-semana';

/** Linha de contexto embaixo de cada indicador, só com o que o painel já traz. */
export function detalhes(p: Painel): {
  emprestados: string;
  atrasados: string;
  separados: string;
} {
  const saidasHoje = p.movimento.at(-1)?.saidas ?? 0;
  const maisAntigo = Math.max(0, ...p.atrasados.map((a) => a.diasAtraso));
  const prazoHoje = p.paraSeparar.filter((r) => r.prazoRetirada <= p.hoje).length;
  return {
    emprestados: saidasHoje === 0 ? 'nenhuma saída hoje' : `+${saidasHoje} hoje`,
    atrasados: maisAntigo === 0 ? 'nenhum em atraso' : `mais antigo há ${dias(maisAntigo)}`,
    separados:
      prazoHoje === 0 ? 'nenhum com prazo hoje' : `${prazoHoje} com prazo hoje`,
  };
}

/** Tela inicial da gestão: o que precisa de atenção hoje e o movimento da semana. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, CapaLivro, MovimentoSemana],
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
      <!-- Faixa única de indicadores: filetes entre eles e a cor de cada um no topo. -->
      <section class="indicadores" aria-label="Indicadores do dia">
        <a class="ind azul" routerLink="/painel/emprestimos">
          <span class="rotulo"><mat-icon aria-hidden="true">swap_horiz</mat-icon>Empréstimos ativos</span>
          <span class="numero">{{ p.contadores.emprestados }}</span>
          <span class="detalhe">{{ det().emprestados }}</span>
        </a>
        <div class="ind amarelo">
          <span class="rotulo"><mat-icon aria-hidden="true">event</mat-icon>Vencimentos ativos</span>
          <span class="numero">{{ p.contadores.venceHoje }}</span>
          <span class="detalhe">devolução prevista para hoje</span>
        </div>
        <a class="ind vermelho" [class.atencao]="p.contadores.atrasados > 0" routerLink="/painel/emprestimos" [queryParams]="{ status: 'ATRASADO' }">
          <span class="rotulo"><mat-icon aria-hidden="true">schedule</mat-icon>Em atraso</span>
          <span class="numero">{{ p.contadores.atrasados }}</span>
          <span class="detalhe">{{ det().atrasados }}</span>
        </a>
        <a class="ind verde" routerLink="/painel/reservas">
          <span class="rotulo"><mat-icon aria-hidden="true">inventory_2</mat-icon>Exemplares separados</span>
          <span class="numero">{{ p.contadores.reservasParaSeparar }}</span>
          <span class="detalhe">{{ det().separados }}</span>
        </a>
        <div class="ind marinho">
          <span class="rotulo"><mat-icon aria-hidden="true">person_off</mat-icon>Leitores bloqueados</span>
          <span class="numero">{{ p.contadores.leitoresBloqueados }}</span>
          <span class="detalhe">não podem retirar livros</span>
        </div>
      </section>

      <div class="grade">
        <div class="coluna-principal">
          <section class="bloco" aria-labelledby="t-separados">
            <header>
              <h2 id="t-separados">Exemplares separados</h2>
              <span class="qt">{{ quantos(p.contadores.reservasParaSeparar, 'reserva', 'reservas') }}</span>
              <a routerLink="/painel/reservas">Ver reservas</a>
            </header>
            @if (p.paraSeparar.length === 0) {
              <p class="vazio">Nenhuma reserva esperando.</p>
            } @else {
              <ul class="linhas">
                @for (r of p.paraSeparar; track r.id) {
                  <li>
                    <app-capa-livro [obraId]="r.obraId" [mini]="true" [titulo]="r.titulo" />
                    <div class="livro">
                      <a class="titulo" [routerLink]="['/painel/acervo', r.obraId]">{{ r.titulo }}</a>
                      <span class="meta">Tombo {{ r.tombo }}{{ r.localizacao ? ' · ' + r.localizacao : '' }}</span>
                    </div>
                    <div class="pessoa">
                      <a [routerLink]="['/painel/leitores', r.leitorId]">{{ r.leitor }}</a>
                      <span class="meta">{{ legivel(r.protocolo) }}</span>
                    </div>
                    <span class="prazo" [class.hoje]="r.prazoRetirada <= p.hoje">
                      {{ r.prazoRetirada <= p.hoje ? 'Retirar hoje' : 'Até ' + diaCurto(r.prazoRetirada) }}
                    </span>
                    <a class="acao" routerLink="/painel/reservas">Entregar</a>
                  </li>
                }
              </ul>
            }
          </section>

          <section class="bloco" aria-labelledby="t-atraso">
            <header>
              <h2 id="t-atraso">Em atraso</h2>
              <span class="qt">{{ quantos(p.contadores.atrasados, 'empréstimo', 'empréstimos') }}</span>
              @if (p.atrasados.length) {
                <a routerLink="/painel/emprestimos" [queryParams]="{ status: 'ATRASADO' }">Ver todos</a>
              }
            </header>
            @if (p.atrasados.length === 0) {
              <p class="vazio">Nenhum empréstimo atrasado.</p>
            } @else {
              <ul class="linhas">
                @for (e of p.atrasados; track e.id) {
                  <li>
                    <app-capa-livro [obraId]="e.obraId" [mini]="true" [titulo]="e.titulo" />
                    <div class="livro">
                      <a class="titulo" [routerLink]="['/painel/acervo', e.obraId]">{{ e.titulo }}</a>
                      <span class="meta">Tombo {{ e.tombo }}</span>
                    </div>
                    <div class="pessoa">
                      <a [routerLink]="['/painel/leitores', e.leitorId]">{{ e.leitor }}</a>
                      @if (e.telefone) {
                        <span class="meta">{{ e.telefone }}</span>
                      }
                    </div>
                    <span class="prazo atraso">{{ dias(e.diasAtraso) }}</span>
                    <a class="acao" routerLink="/painel/devolver" [queryParams]="{ tombo: e.tombo }">Devolver</a>
                  </li>
                }
              </ul>
            }
          </section>
        </div>

        <div class="coluna-lateral">
          <section class="bloco" aria-labelledby="t-vence">
            <header>
              <h2 id="t-vence">Vencimentos ativos</h2>
              <span class="qt">hoje</span>
            </header>
            @if (p.venceHoje.length === 0) {
              <p class="vazio">Nenhuma devolução prevista para hoje.</p>
            } @else {
              <ul class="linhas">
                @for (e of p.venceHoje; track e.id) {
                  <li>
                    <app-capa-livro [obraId]="e.obraId" [mini]="true" [titulo]="e.titulo" />
                    <div class="livro">
                      <a class="titulo" [routerLink]="['/painel/acervo', e.obraId]">{{ e.titulo }}</a>
                      <span class="meta">
                        <a [routerLink]="['/painel/leitores', e.leitorId]">{{ e.leitor }}</a> · Tombo {{ e.tombo }}
                      </span>
                    </div>
                  </li>
                }
              </ul>
              @if (p.contadores.venceHoje > p.venceHoje.length) {
                <p class="vazio">E mais {{ p.contadores.venceHoje - p.venceHoje.length }}.</p>
              }
            }
          </section>

          <section class="bloco" aria-labelledby="t-movimento">
            <header>
              <h2 id="t-movimento">Quadro de movimentações</h2>
              <span class="qt">últimos 7 dias</span>
            </header>
            <div class="grafico">
              <app-movimento-semana [dias]="p.movimento" />
            </div>
          </section>
        </div>
      </div>
    }
  `,
  styles: `
    .cabecalho {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }
    h1 {
      font-size: clamp(24px, 3vw, 30px);
      line-height: 1.2;
    }
    .cabecalho p {
      margin: 4px 0 0;
    }

    .indicadores {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
      gap: 1px;
      background: var(--filete);
      border: 1px solid var(--filete);
    }
    .ind {
      --cor: var(--marca-marinho);
      --cor-texto: var(--mat-sys-on-surface-variant);
      min-width: 0;
      display: flex;
      flex-direction: column;
      padding: 14px 20px 16px;
      background: var(--mat-sys-surface);
      border-top: 4px solid var(--cor);
      color: var(--mat-sys-on-surface-variant);
      text-decoration: none;
    }
    .ind.azul { --cor: var(--prefeitura-azul); }
    .ind.amarelo { --cor: var(--prefeitura-amarelo); }
    .ind.vermelho { --cor: var(--prefeitura-vermelho); --cor-texto: var(--situacao-erro); }
    .ind.verde { --cor: var(--prefeitura-verde); }
    a.ind:hover {
      background: color-mix(in srgb, var(--cor) 6%, var(--mat-sys-surface));
    }
    a.ind:focus-visible {
      outline: 2px solid var(--mat-sys-primary);
      outline-offset: -2px;
    }
    .rotulo {
      display: flex;
      align-items: center;
      gap: 6px;
      font: var(--mat-sys-body-medium);
    }
    .rotulo mat-icon {
      flex: none;
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--cor);
    }
    .numero {
      margin: 6px 0 2px;
      font-size: 38px;
      line-height: 1.1;
      letter-spacing: -0.02em;
      font-variant-numeric: tabular-nums;
      color: var(--mat-sys-on-surface);
    }
    .detalhe {
      font: var(--mat-sys-body-small);
    }
    .ind.atencao .detalhe {
      color: var(--cor-texto);
      font-weight: 500;
    }

    .grade {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(380px, 100%), 1fr));
      gap: 24px;
      margin-top: 24px;
      align-items: start;
    }
    @media (min-width: 1200px) {
      .grade {
        grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
      }
    }
    .coluna-principal,
    .coluna-lateral {
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    /* Linha: capa, livro, leitor, prazo e ação; em tela estreita o leitor e o prazo descem. */
    .linhas {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .linhas li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px 14px;
      padding: 10px 20px;
      border-top: 1px solid var(--filete);
      --largura-capa: 30px;
    }
    .linhas li:first-child {
      border-top: 0;
    }
    app-capa-livro {
      border-radius: 2px;
      box-shadow: 0 1px 2px rgb(0 0 0 / 0.25);
    }
    .livro {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .pessoa {
      flex: 0 1 170px;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .titulo {
      font-weight: 500;
      color: inherit;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .meta {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      font-variant-numeric: tabular-nums;
    }
    .livro a,
    .pessoa a {
      text-decoration: none;
    }
    .pessoa > a {
      color: inherit;
    }
    .livro a:hover,
    .pessoa a:hover {
      text-decoration: underline;
    }
    .prazo {
      flex: 0 0 auto;
      min-width: 88px;
      font: var(--mat-sys-body-medium);
      white-space: nowrap;
    }
    .prazo.hoje {
      color: var(--situacao-alerta);
      font-weight: 500;
    }
    .prazo.atraso {
      color: var(--situacao-erro);
      font-weight: 500;
    }
    .acao {
      flex: none;
      margin-left: auto;
      font: var(--mat-sys-body-medium);
      text-decoration: none;
    }
    .grafico {
      padding: 16px 20px 18px;
    }
  `,
})
export class Inicio {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(Sessao);
  protected readonly painel = signal<Painel | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly det = computed(() => {
    const p = this.painel();
    return p ? detalhes(p) : { emprestados: '', atrasados: '', separados: '' };
  });
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

  /** "Seg., 23/11". */
  protected diaCurto(iso: string): string {
    const d = diaDaSemana(iso);
    return `${d.charAt(0).toUpperCase()}${d.slice(1)}., ${dataBr(iso).slice(0, 5)}`;
  }

  protected quantos(n: number, um: string, varios: string): string {
    return `${n} ${n === 1 ? um : varios}`;
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
