import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Pagina } from '../../core/api/modelos';
import { PaginadorPtBr } from '../../core/paginador-pt-br';
import { ConfiguracoesApi, RegistroAuditoria } from './configuracoes-api';

const ACOES: { valor: string; texto: string }[] = [
  { valor: 'LOGIN', texto: 'Entradas no sistema' },
  { valor: 'LOGIN_RECUSADO', texto: 'Logins recusados' },
  { valor: 'LOGIN_TRAVADO', texto: 'Logins travados' },
  { valor: 'FUNCIONARIO_CADASTRADO', texto: 'Funcionários cadastrados' },
  { valor: 'FUNCIONARIO_ALTERADO', texto: 'Funcionários alterados' },
  { valor: 'SENHA_REDEFINIDA', texto: 'Senhas redefinidas' },
  { valor: 'SENHA_TROCADA', texto: 'Senhas trocadas' },
  { valor: 'PARAMETRO_ALTERADO', texto: 'Prazos e limites' },
  { valor: 'FERIADO_INCLUIDO', texto: 'Feriados incluídos' },
  { valor: 'FERIADO_REMOVIDO', texto: 'Feriados removidos' },
  { valor: 'FERIADOS_IMPORTADOS', texto: 'Feriados nacionais importados' },
  { valor: 'RESERVA_CANCELADA', texto: 'Reservas canceladas na gestão' },
  { valor: 'EXEMPLAR_ALTERADO', texto: 'Baixas e extravios' },
  { valor: 'LEITOR_DESBLOQUEADO', texto: 'Desbloqueios de leitor' },
];

/** Quem fez o quê nas ações sensíveis. Só leitura. */
@Component({
  selector: 'app-aba-auditoria',
  imports: [DatePipe, MatFormFieldModule, MatIconModule, MatPaginatorModule, MatSelectModule],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  template: `
    <mat-form-field appearance="outline" class="filtro" subscriptSizing="dynamic">
      <mat-label>Mostrar</mat-label>
      <mat-select [value]="acao()" (valueChange)="filtrar($event)">
        <mat-option [value]="null">Tudo</mat-option>
        @for (a of acoes; track a.valor) {
          <mat-option [value]="a.valor">{{ a.texto }}</mat-option>
        }
      </mat-select>
    </mat-form-field>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (resultado(); as r) {
      @if (r.total === 0) {
        <p class="texto-secundario">Nada registrado.</p>
      } @else {
        <ul class="lista">
          @for (a of r.itens; track a.id) {
            <li>
              <span class="quando">{{ a.dataHora | date: 'dd/MM/yyyy HH:mm' }}</span>
              <div class="info">
                <span>{{ a.descricao }}{{ a.detalhe ? ': ' + a.detalhe : '' }}</span>
                <span class="texto-secundario">{{ a.funcionario ?? 'Sem funcionário identificado' }}</span>
              </div>
            </li>
          }
        </ul>
        <mat-paginator [length]="r.total" [pageIndex]="r.pagina" [pageSize]="30" [hidePageSize]="true" (page)="mudarPagina($event)" />
      }
    }
  `,
  styles: `
    :host {
      display: block;
      padding-top: 16px;
    }
    .filtro {
      width: 100%;
      max-width: 320px;
      margin-bottom: 8px;
    }
    .lista {
      list-style: none;
      padding: 0;
      margin: 0;
      max-width: 900px;
    }
    .lista li {
      display: flex;
      gap: 16px;
      padding: 8px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .quando {
      flex: none;
      width: 128px;
      font-variant-numeric: tabular-nums;
      color: var(--mat-sys-on-surface-variant);
    }
    .info {
      display: flex;
      flex-direction: column;
      min-width: 0;
      overflow-wrap: anywhere;
    }
  `,
})
export class AbaAuditoria {
  private readonly api = inject(ConfiguracoesApi);
  protected readonly acoes = ACOES;
  protected readonly acao = signal<string | null>(null);
  protected readonly pagina = signal(0);
  protected readonly resultado = signal<Pagina<RegistroAuditoria> | null>(null);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    effect(() => {
      const acao = this.acao();
      const pagina = this.pagina();
      this.api.auditoria(acao, pagina).subscribe({
        next: (r) => this.resultado.set(r),
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
    });
  }

  protected filtrar(acao: string | null): void {
    this.acao.set(acao);
    this.pagina.set(0);
  }

  protected mudarPagina(e: PageEvent): void {
    this.pagina.set(e.pageIndex);
  }
}
