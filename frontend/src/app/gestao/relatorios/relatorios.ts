import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { hojeIso } from '../reservas/reservas-api';

interface Relatorio {
  caminho: string;
  titulo: string;
  descricao: string;
  icone: string;
  periodo: boolean;
}

export const RELATORIOS: Relatorio[] = [
  {
    caminho: 'emprestimos',
    titulo: 'Empréstimos',
    descricao: 'Todos os empréstimos retirados no período, com situação, devolução e leitor.',
    icone: 'output',
    periodo: true,
  },
  {
    caminho: 'atrasados',
    titulo: 'Atrasados',
    descricao: 'Empréstimos atrasados, com e-mail e contato.',
    icone: 'schedule',
    periodo: false,
  },
  {
    caminho: 'mais-emprestados',
    titulo: 'Mais retirados',
    descricao: 'Os 100 títulos com mais empréstimos no período.',
    icone: 'trending_up',
    periodo: true,
  },
  {
    caminho: 'reservas',
    titulo: 'Reservas',
    descricao: 'Reservas realizadas pelo site no período e status de conclusão.',
    icone: 'bookmark',
    periodo: true,
  },
  {
    caminho: 'buscas-sem-resultado',
    titulo: 'Buscas sem resultado',
    descricao: 'O que os visitantes procuraram no site e não encontraram no acervo: ajuda a decidir o que adquirir.',
    icone: 'search_off',
    periodo: true,
  },
  {
    caminho: 'acervo',
    titulo: 'Acervo',
    descricao: 'Todos os exemplares, com tombo, localização, estado e situação.',
    icone: 'library_books',
    periodo: false,
  },
];

/** "2026-11-17" menos n dias, sem fuso. */
export function diasAntes(iso: string, n: number): string {
  const [a, m, d] = iso.split('-').map(Number);
  const x = new Date(a, m - 1, d - n);
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`;
}

/** Nome do arquivo no Content-Disposition (filename="..."). */
export function nomeDoArquivo(disposicao: string | null, padrao: string): string {
  const m = /filename="?([^";]+)"?/.exec(disposicao ?? '');
  return m ? m[1] : padrao;
}

/**
 * Relatórios em CSV (abrem no Excel e no LibreOffice). O download passa pelo HttpClient porque a API exige o
 * token; cada exportação fica registrada na auditoria.
 */
@Component({
  selector: 'app-relatorios',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <h1>Relatórios</h1>
    <p class="texto-secundario intro">Os arquivos são retirados em CSV e exibidos através do Excel ou no LibreOffice.</p>

    <section class="periodo" aria-labelledby="titulo-periodo">
      <h2 id="titulo-periodo">Período</h2>
      <p class="texto-secundario">Referente a Empréstimos, Mais retirados, Reservas e Buscas sem resultado.</p>
      <mat-form-field appearance="outline">
        <mat-label>De</mat-label>
        <input matInput type="date" [(ngModel)]="de" [max]="ate" required />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Até</mat-label>
        <input matInput type="date" [(ngModel)]="ate" [min]="de" [max]="hoje" required />
      </mat-form-field>
      <div class="atalhos">
        <button mat-stroked-button type="button" (click)="ultimos(7)">7 dias</button>
        <button mat-stroked-button type="button" (click)="ultimos(30)">30 dias</button>
        <button mat-stroked-button type="button" (click)="esteAno()">Este ano</button>
      </div>
    </section>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }

    <ul class="relatorios">
      @for (r of relatorios; track r.caminho) {
        <li>
          <mat-icon class="icone">{{ r.icone }}</mat-icon>
          <div class="info">
            <strong>{{ r.titulo }}</strong>
            <span class="texto-secundario">{{ r.descricao }}</span>
          </div>
          <button mat-flat-button type="button" [disabled]="baixando() !== null" (click)="baixar(r)">
            <mat-icon>download</mat-icon> {{ baixando() === r.caminho ? 'Gerando...' : 'Baixar' }}
          </button>
        </li>
      }
    </ul>
  `,
  styles: `
    .intro {
      margin: 4px 0 20px;
    }
    .periodo {
      display: flex;
      flex-direction: column;
      max-width: 360px;
      margin-bottom: 16px;
    }
    .periodo h2 {
      margin: 0;
    }
    .periodo p {
      margin: 2px 0 12px;
    }
    .atalhos {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .aviso {
      max-width: 760px;
    }
    .relatorios {
      list-style: none;
      padding: 0;
      margin: 8px 0 0;
      max-width: 760px;
    }
    .relatorios li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
      padding: 14px 4px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .icone {
      color: var(--mat-sys-primary);
    }
    .info {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    @media (max-width: 520px) {
      .relatorios li {
        flex-wrap: wrap;
      }
    }
  `,
})
export class Relatorios {
  private readonly http = inject(HttpClient);
  protected readonly relatorios = RELATORIOS;
  protected readonly hoje = hojeIso();
  protected de = diasAntes(this.hoje, 29);
  protected ate = this.hoje;
  protected readonly baixando = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);

  protected ultimos(n: number): void {
    this.ate = this.hoje;
    this.de = diasAntes(this.hoje, n - 1);
  }

  protected esteAno(): void {
    this.ate = this.hoje;
    this.de = `${this.hoje.slice(0, 4)}-01-01`;
  }

  protected async baixar(r: Relatorio): Promise<void> {
    this.erro.set(null);
    this.mensagem.set(null);
    if (r.periodo && (!this.de || !this.ate)) {
      this.erro.set('Informe as datas do período.');
      return;
    }
    // O campo de data aceita qualquer ano (até 0221): confere antes de pedir o arquivo.
    if (r.periodo && (this.de < '2000-01-01' || this.ate > this.hoje || this.de > this.ate)) {
      this.erro.set('Período inválido. Confira o dia, o mês e o ano das duas datas.');
      return;
    }
    let params = new HttpParams();
    if (r.periodo) params = params.set('de', this.de).set('ate', this.ate);
    this.baixando.set(r.caminho);
    try {
      const resposta = await firstValueFrom(
        this.http.get(`/api/v1/relatorios/${r.caminho}.csv`, { params, responseType: 'blob', observe: 'response' }),
      );
      const nome = nomeDoArquivo(resposta.headers.get('Content-Disposition'), `${r.caminho}.csv`);
      const url = URL.createObjectURL(resposta.body!);
      const a = document.createElement('a');
      a.href = url;
      a.download = nome;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      this.mensagem.set(`${r.titulo}: arquivo ${nome} baixado.`);
    } catch (e) {
      this.erro.set(mensagemDeErro(await comCorpoJson(e)));
    } finally {
      this.baixando.set(null);
    }
  }
}

/** Com responseType 'blob', o erro da API também chega como Blob: converte para o JSON do ProblemDetail. */
async function comCorpoJson(e: unknown): Promise<unknown> {
  if (e instanceof HttpErrorResponse && e.error instanceof Blob) {
    try {
      const corpo = JSON.parse(await e.error.text());
      return new HttpErrorResponse({ error: corpo, status: e.status, statusText: e.statusText, url: e.url ?? undefined });
    } catch {
      return e;
    }
  }
  return e;
}
