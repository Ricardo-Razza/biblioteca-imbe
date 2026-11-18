import { Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Emprestimo, Leitor, SituacaoLeitor } from '../../core/api/modelos';
import { dataBr, formatarCpf } from '../../shared/formatos';
import { Selo } from '../../shared/selo/selo';
import { EmprestimosApi } from '../emprestimos/emprestimos-api';
import { ItemEmprestimo } from '../emprestimos/item-emprestimo';
import { Sessao } from '../../core/auth/sessao';
import { LeitoresApi } from './leitores-api';

/** Ficha do leitor: dados, se pode emprestar, livros com ele e histórico. */
@Component({
  selector: 'app-ficha-leitor',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, ItemEmprestimo, Selo],
  template: `
    <a mat-button routerLink="/painel/leitores" class="voltar"><mat-icon>arrow_back</mat-icon> Leitores</a>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    @if (leitor(); as l) {
      <div class="cabecalho">
        <div>
          <h1>{{ l.nome }}</h1>
          <p class="texto-secundario">CPF {{ cpf(l.cpf) }} · nascimento em {{ data(l.dataNascimento) }}</p>
        </div>
        <div class="acoes">
          @if (situacao()?.podeEmprestar) {
            <a mat-flat-button routerLink="/painel/emprestar" [queryParams]="{ leitor: l.id }"><mat-icon>output</mat-icon> Emprestar</a>
          }
          <a mat-stroked-button [routerLink]="['/painel/leitores', l.id, 'editar']"><mat-icon>edit</mat-icon> Editar</a>
        </div>
      </div>

      <div class="selos">
        @if (!l.ativo) {
          <app-selo tom="neutra">Desativado</app-selo>
        }
        @if (l.cadastroPendente) {
          <app-selo tom="alerta">Cadastro pendente</app-selo>
        }
        @if (l.menorDeIdade) {
          <app-selo tom="neutra">Menor de idade{{ l.responsavel ? ' · responsável: ' + l.responsavel : '' }}</app-selo>
        }
      </div>

      @if (l.cadastroPendente) {
        <div class="aviso alerta" role="status">
          <mat-icon>how_to_reg</mat-icon>
          <span>
            Cadastro feito pelo próprio leitor no site, ao reservar. Confira os dados com um documento com foto,
            complete o que faltar e salve. Antes disso, ele não pode levar livros.
          </span>
          <a mat-button [routerLink]="['/painel/leitores', l.id, 'editar']">Conferir cadastro</a>
        </div>
      }

      @if (situacao(); as s) {
        @if (s.podeEmprestar) {
          <p class="aviso ok" role="status">
            <mat-icon>check_circle</mat-icon>
            <span>Pode emprestar: está com {{ s.livrosEmUso }} de {{ s.limite }} livros.</span>
          </p>
        } @else {
          <div class="aviso alerta" role="status">
            <mat-icon>block</mat-icon>
            <span>
              Não pode emprestar agora.
              @for (m of s.motivos; track m) {
                {{ m }}
              }
            </span>
            @if (l.bloqueadoAte && sessao.admin()) {
              <button mat-button type="button" (click)="desbloquear()" [disabled]="desbloqueando()">Desbloquear</button>
            }
          </div>
        }
      }

      <dl class="dados">
        @if (l.telefone) {
          <dt>Telefone</dt>
          <dd>{{ l.telefone }}</dd>
        }
        @if (l.email) {
          <dt>E-mail</dt>
          <dd>{{ l.email }}</dd>
        }
        @if (l.endereco) {
          <dt>Endereço</dt>
          <dd>{{ l.endereco }}</dd>
        }
        <dt>Cadastrado em</dt>
        <dd>{{ data(l.criadoEm) }}</dd>
      </dl>

      <h2>Com o leitor</h2>
      @if (mensagem()) {
        <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
      }
      @if (erroAcao()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erroAcao() }}</p>
      }
      @if (abertos().length) {
        @for (e of abertos(); track e.id) {
          <app-item-emprestimo [e]="e" [mostrarLeitor]="false">
            @if (e.status === 'ATIVO' && e.renovacoesRestantes > 0) {
              <button mat-button type="button" (click)="renovar(e)" [disabled]="ocupado()">Renovar</button>
            }
            <a mat-stroked-button routerLink="/painel/devolver" [queryParams]="{ tombo: e.exemplar.tombo }">Devolver</a>
          </app-item-emprestimo>
        }
      } @else {
        <p class="texto-secundario">Nenhum livro emprestado.</p>
      }

      <h2>Histórico</h2>
      @if (historico().length) {
        @for (e of historico(); track e.id) {
          <app-item-emprestimo [e]="e" [mostrarLeitor]="false" />
        }
        @if (totalHistorico() > historico().length) {
          <p class="texto-secundario">Mostrando os {{ historico().length }} mais recentes de {{ totalHistorico() }}.</p>
        }
      } @else {
        <p class="texto-secundario">Nenhuma devolução ainda.</p>
      }
    } @else if (!erro()) {
      <mat-spinner diameter="32" />
    }
  `,
  styles: `
    .voltar {
      margin: 0 0 12px -12px;
    }
    .cabecalho {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
    }
    .cabecalho p {
      margin: 4px 0 0;
    }
    .selos {
      display: flex;
      gap: 8px;
      margin: 12px 0;
    }
    .aviso {
      max-width: 760px;
      margin: 12px 0;
    }
    .dados {
      display: grid;
      grid-template-columns: max-content minmax(0, 1fr);
      gap: 4px 16px;
      margin: 16px 0 24px;
    }
    dt {
      color: var(--mat-sys-on-surface-variant);
    }
    dd {
      margin: 0;
      overflow-wrap: anywhere;
    }
    h2 {
      margin: 24px 0 8px;
    }
  `,
})
export class FichaLeitor {
  readonly id = input.required({ transform: numberAttribute });

  private readonly leitores = inject(LeitoresApi);
  protected readonly sessao = inject(Sessao);
  private readonly emprestimos = inject(EmprestimosApi);

  protected readonly leitor = signal<Leitor | null>(null);
  protected readonly situacao = signal<SituacaoLeitor | null>(null);
  protected readonly abertos = signal<Emprestimo[]>([]);
  protected readonly historico = signal<Emprestimo[]>([]);
  protected readonly totalHistorico = signal(0);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroAcao = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly desbloqueando = signal(false);
  protected readonly cpf = formatarCpf;
  protected readonly data = dataBr;
  protected readonly atrasados = computed(() => this.abertos().filter((e) => e.status === 'ATRASADO').length);

  constructor() {
    effect(() => this.carregar(this.id()));
  }

  protected renovar(e: Emprestimo): void {
    this.ocupado.set(true);
    this.erroAcao.set(null);
    this.mensagem.set(null);
    this.emprestimos.renovar(e.id).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.mensagem.set(`Renovado. Nova devolução em ${dataBr(r.dataPrevista)}.`);
        this.carregar(this.id());
      },
      error: (err) => {
        this.ocupado.set(false);
        this.erroAcao.set(mensagemDeErro(err));
      },
    });
  }

  protected desbloquear(): void {
    this.desbloqueando.set(true);
    this.leitores.desbloquear(this.id()).subscribe({
      next: () => {
        this.desbloqueando.set(false);
        this.carregar(this.id());
      },
      error: (err) => {
        this.desbloqueando.set(false);
        this.erroAcao.set(mensagemDeErro(err));
      },
    });
  }

  private carregar(id: number): void {
    this.erro.set(null);
    forkJoin({
      leitor: this.leitores.buscar(id),
      situacao: this.leitores.situacao(id),
      abertos: this.emprestimos.listar({ status: 'ATIVO', leitorId: id, tamanho: 50 }),
      historico: this.emprestimos.listar({ status: 'DEVOLVIDO', leitorId: id, tamanho: 20 }),
    }).subscribe({
      next: (r) => {
        this.leitor.set(r.leitor);
        this.situacao.set(r.situacao);
        this.abertos.set(r.abertos.itens);
        this.historico.set(r.historico.itens);
        this.totalHistorico.set(r.historico.total);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
