import { Component, ElementRef, Injector, afterNextRender, effect, inject, input, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Emprestimo, Exemplar, Leitor, SituacaoLeitor } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { SeloSituacao } from '../../shared/selo-situacao/selo-situacao';
import { AcervoApi } from '../acervo/acervo-api';
import { BuscaLeitor } from '../../shared/busca-leitor/busca-leitor';
import { dataBr, formatarCpf } from '../../shared/formatos';
import { ItemEmprestimo } from '../emprestimos/item-emprestimo';
import { LeitoresApi } from '../leitores/leitores-api';
import { EmprestimosApi } from '../emprestimos/emprestimos-api';

/**
 * Emprestar no balcão: escolhe o leitor, confere se pode e informa os livros, um atrás do outro. Nem todo livro
 * tem etiqueta: o campo aceita o tombo (bipado ou digitado), o ISBN, um trecho do título ou o autor. Tombo exato
 * empresta direto; o resto mostra os exemplares encontrados para escolher.
 * `?leitor=` abre com o leitor já escolhido (vindo da ficha).
 */
@Component({
  selector: 'app-emprestar',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    BuscaLeitor,
    CapaLivro,
    ItemEmprestimo,
    SeloSituacao,
  ],
  template: `
    <h1>Empréstimo</h1>

    @if (!situacao()) {
      <p class="texto-secundario">Procure o leitor pelo nome ou pelo CPF.</p>
      <app-busca-leitor [focarAoAbrir]="true" (selecionado)="escolher($event.id)" />
      <p class="texto-secundario novo">Não tem cadastro? <a routerLink="/painel/leitores/novo">Cadastrar leitor</a></p>
      @if (erro()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
      }
    } @else {
      @let s = situacao()!;
      <div class="leitor">
        <div>
          <a class="nome" [routerLink]="['/painel/leitores', s.leitorId]">{{ s.nome }}</a>
          @if (cpfLeitor()) {
            <span class="texto-secundario"> · CPF {{ cpfLeitor() }}</span>
          }
          <p class="texto-secundario">Com {{ s.livrosEmUso }} de {{ s.limite }} livros{{ s.reservasAguardando ? ' (contando ' + s.reservasAguardando + ' reserva)' : '' }}.</p>
        </div>
        <button mat-button type="button" (click)="trocarLeitor()"><mat-icon>swap_horiz</mat-icon> Trocar leitor</button>
      </div>

      @if (!s.podeEmprestar) {
        <div class="aviso alerta" role="status">
          <mat-icon>block</mat-icon>
          <span>
            @for (m of s.motivos; track m) {
              {{ m }}
            }
          </span>
        </div>
      } @else {
        <form class="tombo" (submit)="emprestar($event)">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Livro</mat-label>
            <mat-icon matPrefix>barcode_scanner</mat-icon>
            <input #campoTombo matInput [formControl]="tombo" autocomplete="off" placeholder="Tombo, ISBN, título ou autor" maxlength="100" />
            <mat-hint>Bipe a etiqueta ou digite tombo, ISBN, título ou autor.</mat-hint>
            <mat-error>{{ erroTombo() }}</mat-error>
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="enviando()">
            @if (enviando()) {
              <mat-spinner diameter="18" />
            }
            Emprestar
          </button>
        </form>

        @if (encontrados(); as lista) {
          <section class="encontrados" aria-live="polite">
            <h2>{{ lista.length === 1 ? '1 exemplar encontrado' : lista.length + ' exemplares encontrados' }}</h2>
            @for (x of lista; track x.id) {
              <div class="linha">
                <app-capa-livro class="capa" [obraId]="x.obra.id" [mini]="true" [titulo]="x.obra.titulo" />
                <div class="info">
                  <span class="titulo">{{ x.obra.titulo }}</span>
                  <span class="texto-secundario">{{ x.obra.autores.join(', ') }}</span>
                  <span class="texto-secundario">Tombo {{ x.tombo }}{{ x.localizacao ? ' · ' + x.localizacao : '' }}</span>
                </div>
                <app-selo-situacao [situacao]="x.situacao" />
                <button mat-stroked-button type="button" [disabled]="enviando()" (click)="emprestarTombo(x.tombo)">Emprestar</button>
              </div>
            }
            <button mat-button type="button" (click)="limparBusca()"><mat-icon>close</mat-icon> Limpar a busca</button>
          </section>
        }
      }

      @if (feitos().length) {
        <h2>Emprestados agora</h2>
        @for (e of feitos(); track e.id) {
          <app-item-emprestimo [e]="e" [mostrarLeitor]="false" />
        }
        <p class="aviso ok" role="status">
          <mat-icon>event</mat-icon>
          <span>Devolução até {{ data(feitos()[0].dataPrevista) }}. Avise o leitor.</span>
        </p>
        <div class="acoes">
          <button mat-flat-button type="button" (click)="trocarLeitor()">Concluir</button>
        </div>
      }
    }
  `,
  styles: `
    h1 {
      margin-bottom: 16px;
    }
    h2 {
      margin: 24px 0 8px;
    }
    .novo {
      margin-top: 12px;
    }
    .leitor {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      max-width: 760px;
      padding: 12px 16px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      margin-bottom: 16px;
    }
    .leitor p {
      margin: 4px 0 0;
    }
    .nome {
      font: var(--mat-sys-title-medium);
      color: inherit;
    }
    .aviso {
      max-width: 760px;
      margin: 12px 0;
    }
    .tombo {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: flex-start;
      max-width: 640px;
    }
    .tombo mat-form-field {
      flex: 1 1 240px;
      min-width: 0;
    }
    .encontrados {
      max-width: 760px;
      margin-top: 16px;
    }
    .encontrados h2 {
      margin: 0 0 8px;
      font-size: 17px;
    }
    .linha {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px 14px;
      padding: 10px 12px;
      border-bottom: 1px solid var(--filete);
      background: var(--mat-sys-surface);
    }
    .linha .capa {
      --largura-capa: 34px;
      flex: none;
    }
    .info {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .titulo {
      font-weight: 600;
    }
    .tombo button {
      margin-top: 4px;
      height: 48px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class Emprestar {
  readonly leitor = input<string>();

  private readonly leitores = inject(LeitoresApi);
  private readonly acervo = inject(AcervoApi);
  private readonly emprestimos = inject(EmprestimosApi);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly campoTombo = viewChild<ElementRef<HTMLInputElement>>('campoTombo');

  protected readonly situacao = signal<SituacaoLeitor | null>(null);
  protected readonly cpfLeitor = signal('');
  protected readonly feitos = signal<Emprestimo[]>([]);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroTombo = signal('');
  /** Exemplares achados por título, autor ou ISBN, para escolher (nulo: sem busca aberta). */
  protected readonly encontrados = signal<Exemplar[] | null>(null);
  protected readonly tombo = new FormControl('', { nonNullable: true });
  protected readonly data = dataBr;

  constructor() {
    effect(() => {
      const id = Number(this.leitor());
      if (id) this.escolher(id);
    });
  }

  protected escolher(id: number): void {
    this.erro.set(null);
    this.feitos.set([]);
    this.leitores.buscar(id).subscribe({ next: (l: Leitor) => this.cpfLeitor.set(formatarCpf(l.cpf)) });
    this.atualizarSituacao(id, true);
  }

  protected trocarLeitor(): void {
    this.situacao.set(null);
    this.feitos.set([]);
    this.limparBusca();
    if (this.leitor()) {
      this.router.navigate([], { queryParams: { leitor: null }, replaceUrl: true });
    }
  }

  /** Tombo exato empresta direto (o leitor de código de barras continua igual); o resto vira lista para escolher. */
  protected emprestar(evento: Event): void {
    evento.preventDefault();
    const termo = this.tombo.value.trim();
    if (!this.situacao() || !termo || this.enviando()) {
      return;
    }
    if (termo.length < 2) {
      this.mostrarErro('Digite pelo menos 2 letras do título ou do autor.');
      return;
    }
    this.enviando.set(true);
    this.acervo.buscarParaEmprestimo(termo).subscribe({
      next: (lista) => {
        this.enviando.set(false);
        const exato = lista.find((x) => x.tombo === termo.toUpperCase());
        if (exato) {
          this.emprestarTombo(exato.tombo);
        } else if (!lista.length) {
          this.encontrados.set(null);
          this.mostrarErro(`Nenhum livro disponível encontrado para "${termo}".`);
        } else {
          this.encontrados.set(lista);
        }
      },
      error: (err) => {
        this.enviando.set(false);
        this.mostrarErro(mensagemDeErro(err));
      },
    });
  }

  protected emprestarTombo(tombo: string): void {
    const s = this.situacao();
    if (!s || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.emprestimos.emprestar(s.leitorId, tombo).subscribe({
      next: (e) => {
        this.enviando.set(false);
        this.feitos.update((l) => [e, ...l]);
        this.limparBusca();
        this.atualizarSituacao(s.leitorId, true);
      },
      error: (err) => {
        this.enviando.set(false);
        this.mostrarErro(mensagemDeErro(err));
      },
    });
  }

  protected limparBusca(): void {
    this.encontrados.set(null);
    this.tombo.reset('');
  }

  private mostrarErro(mensagem: string): void {
    this.erroTombo.set(mensagem);
    this.tombo.setErrors({ servidor: true });
    this.tombo.markAsTouched();
    this.campoTombo()?.nativeElement.select();
  }

  private atualizarSituacao(id: number, focar: boolean): void {
    this.leitores.situacao(id).subscribe({
      next: (s) => {
        this.situacao.set(s);
        if (focar) afterNextRender(() => this.campoTombo()?.nativeElement.focus(), { injector: this.injector });
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
