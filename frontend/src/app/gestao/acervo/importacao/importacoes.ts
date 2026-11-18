import { DatePipe } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { mensagemDeErro } from '../../../core/api/erro-api';
import { Pagina } from '../../../core/api/modelos';
import { Selo } from '../../../shared/selo/selo';
import { Importacao, ImportacaoApi, progresso } from './importacao-api';

/** Carga do acervo por planilha: instruções, envio do arquivo e as importações anteriores. Só administrador. */
@Component({
  selector: 'app-importacoes',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, Selo],
  template: `
    <a mat-button routerLink="/painel/acervo" class="voltar"><mat-icon>arrow_back</mat-icon> Acervo</a>
    <h1>Importar planilha</h1>

    <section class="instrucoes">
      <p>Para a carga inicial do acervo. Cada linha é um exemplar:</p>
      <pre>ISBN;Tombo;Localização
9788535914849;000123;Estante 4
9788535914849;000124;Estante 4
8535902775;000125</pre>
      <ul>
        <li>Tombo e localização são opcionais. Sem tombo, só a obra é cadastrada.</li>
        <li>O sistema busca título, autores e capa pelo ISBN, como no cadastro de um livro.</li>
        <li>ISBN que nenhuma fonte conhece fica de fora, para cadastrar à mão.</li>
        <li>No Excel, formate a coluna do ISBN como texto e salve como "CSV (separado por vírgulas)". Até 5.000 linhas por arquivo.</li>
      </ul>
    </section>

    <div class="envio">
      <input #arquivo type="file" accept=".csv,.txt,text/csv,text/plain" (change)="escolher($event)" hidden />
      <button mat-stroked-button type="button" (click)="arquivoInput().nativeElement.click()" [disabled]="enviando()">
        <mat-icon>attach_file</mat-icon> {{ escolhido() ? 'Trocar arquivo' : 'Escolher arquivo' }}
      </button>
      @if (escolhido(); as f) {
        <span class="nome">{{ f.name }}</span>
        <button mat-flat-button type="button" (click)="enviar(f)" [disabled]="enviando()">
          <mat-icon>upload</mat-icon> {{ enviando() ? 'Enviando...' : 'Importar' }}
        </button>
      }
    </div>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    <h2>Importações</h2>
    @if (lista(); as l) {
      @if (l.total === 0) {
        <p class="texto-secundario">Nenhuma planilha importada ainda.</p>
      } @else {
        <ul class="lista">
          @for (i of l.itens; track i.id) {
            <li>
              <a [routerLink]="[i.id]" class="info">
                <strong>{{ i.arquivo }}</strong>
                <span class="texto-secundario">
                  {{ i.criadaEm | date: 'dd/MM/yyyy HH:mm' }}{{ i.funcionario ? ' · ' + i.funcionario : '' }} ·
                  {{ i.total }} {{ i.total === 1 ? 'linha' : 'linhas' }}
                </span>
              </a>
              @if (i.status === 'PROCESSANDO') {
                <app-selo tom="alerta">Processando {{ pct(i) }}%</app-selo>
              } @else {
                <app-selo tom="ok">{{ i.contagem.CADASTRADO + i.contagem.EXEMPLAR_ADICIONADO }} incluídos</app-selo>
              }
            </li>
          }
        </ul>
      }
    }
  `,
  styles: `
    .voltar {
      margin-left: -12px;
    }
    .instrucoes {
      max-width: 720px;
    }
    pre {
      padding: 12px 16px;
      border-radius: 8px;
      background: var(--mat-sys-surface-container);
      font-size: 14px;
      overflow-x: auto;
    }
    ul:not(.lista) {
      padding-left: 20px;
    }
    .envio {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      margin: 16px 0;
    }
    .nome {
      font-weight: 500;
      word-break: break-all;
    }
    h2 {
      margin-top: 32px;
    }
    .lista {
      list-style: none;
      padding: 0;
      max-width: 760px;
    }
    .lista li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
      padding: 10px 4px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .info {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
      color: inherit;
      text-decoration: none;
    }
    .info:hover strong {
      text-decoration: underline;
    }
  `,
})
export class Importacoes {
  private readonly api = inject(ImportacaoApi);
  private readonly router = inject(Router);
  protected readonly arquivoInput = viewChild.required<ElementRef<HTMLInputElement>>('arquivo');
  protected readonly lista = signal<Pagina<Importacao> | null>(null);
  protected readonly escolhido = signal<File | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly pct = progresso;

  constructor() {
    this.api.listar().subscribe({
      next: (l) => this.lista.set(l),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected escolher(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.erro.set(null);
    this.escolhido.set(input.files?.[0] ?? null);
    input.value = '';
  }

  protected enviar(f: File): void {
    this.enviando.set(true);
    this.erro.set(null);
    this.api.enviar(f).subscribe({
      next: (i) => this.router.navigate(['/painel/acervo/importar', i.id]),
      error: (e) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}
