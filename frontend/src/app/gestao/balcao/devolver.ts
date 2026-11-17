import { Component, ElementRef, Injector, afterNextRender, effect, inject, input, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Emprestimo, OcorrenciaDevolucao } from '../../core/api/modelos';
import { dataBr, dias } from '../../shared/formatos';
import { ItemEmprestimo } from '../emprestimos/item-emprestimo';
import { EmprestimosApi } from '../emprestimos/emprestimos-api';

type Ocorrencia = OcorrenciaDevolucao | 'NENHUMA';

/**
 * Devolver no balcão: bipa o tombo, confere o empréstimo e confirma (Enter confirma).
 * `?tombo=` abre com o tombo já buscado (vindo das listas).
 */
@Component({
  selector: 'app-devolver',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    ItemEmprestimo,
  ],
  template: `
    <h1>Devolver</h1>

    <form class="tombo" (submit)="buscar($event)">
      <mat-form-field appearance="outline">
        <mat-label>Tombo</mat-label>
        <mat-icon matPrefix>barcode_scanner</mat-icon>
        <input #campoTombo matInput [formControl]="tombo" autocomplete="off" />
        <mat-hint>Bipe a etiqueta do livro devolvido.</mat-hint>
        <mat-error>{{ erroTombo() }}</mat-error>
      </mat-form-field>
      <button mat-stroked-button type="submit" [disabled]="buscando()">Buscar</button>
    </form>

    @if (emprestimo(); as e) {
      <section class="conferencia">
        <app-item-emprestimo [e]="e" />
        @if (e.diasAtraso > 0) {
          <p class="aviso alerta" role="status">
            <mat-icon>schedule</mat-icon>
            <span>Atrasado {{ dias(e.diasAtraso) }}. Ao devolver, o leitor fica bloqueado por {{ dias(e.diasAtraso) }}, contando hoje.</span>
          </p>
        }
        <form class="coluna" (submit)="confirmar($event)">
          <mat-radio-group [formControl]="ocorrencia" class="opcoes" aria-label="Como o livro voltou">
            <mat-radio-button value="NENHUMA">Voltou em ordem</mat-radio-button>
            <mat-radio-button value="DANIFICADO">Voltou danificado</mat-radio-button>
            <mat-radio-button value="EXTRAVIADO">O leitor perdeu o livro</mat-radio-button>
          </mat-radio-group>
          @if (ocorrencia.value !== 'NENHUMA') {
            <mat-form-field appearance="outline">
              <mat-label>Observação</mat-label>
              <input matInput [formControl]="observacao" maxlength="500" />
              <mat-hint>Ex.: capa rasgada, páginas molhadas.</mat-hint>
            </mat-form-field>
          }
          @if (erro()) {
            <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
          }
          <div class="acoes">
            <button #botaoConfirmar mat-flat-button type="submit" [disabled]="enviando()">
              @if (enviando()) {
                <mat-spinner diameter="18" />
              }
              Confirmar devolução
            </button>
            <button mat-button type="button" (click)="cancelar()">Cancelar</button>
          </div>
        </form>
      </section>
    }

    @if (feitos().length) {
      <h2>Devolvidos agora</h2>
      @for (f of feitos(); track f.e.id) {
        <app-item-emprestimo [e]="f.e" />
        @if (f.e.bloqueadoAte) {
          <p class="aviso alerta bloqueio">
            <mat-icon>block</mat-icon>
            <span>Pelo atraso, {{ f.e.leitor.nome }} não pode emprestar até {{ data(f.e.bloqueadoAte) }}.</span>
          </p>
        }
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
    .tombo {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      max-width: 640px;
    }
    .tombo mat-form-field {
      flex: 1;
    }
    .tombo button {
      margin-top: 4px;
      height: 48px;
    }
    .conferencia {
      max-width: 760px;
      padding: 8px 16px 16px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      margin-top: 8px;
    }
    .opcoes {
      display: flex;
      flex-direction: column;
      margin: 12px 0;
    }
    .aviso {
      margin: 12px 0;
    }
    .bloqueio {
      max-width: 760px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class Devolver {
  readonly tomboInicial = input<string>(undefined, { alias: 'tombo' });

  private readonly api = inject(EmprestimosApi);
  private readonly injector = inject(Injector);
  private readonly campoTombo = viewChild.required<ElementRef<HTMLInputElement>>('campoTombo');
  private readonly botaoConfirmar = viewChild('botaoConfirmar', { read: ElementRef<HTMLButtonElement> });

  protected readonly tombo = new FormControl('', { nonNullable: true });
  protected readonly ocorrencia = new FormControl<Ocorrencia>('NENHUMA', { nonNullable: true });
  protected readonly observacao = new FormControl('', { nonNullable: true });
  protected readonly emprestimo = signal<Emprestimo | null>(null);
  protected readonly feitos = signal<{ e: Emprestimo }[]>([]);
  protected readonly buscando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroTombo = signal('');
  protected readonly data = dataBr;
  protected readonly dias = dias;

  constructor() {
    afterNextRender(() => this.campoTombo().nativeElement.focus());
    effect(() => {
      const t = this.tomboInicial();
      if (t) {
        this.tombo.setValue(t);
        this.procurar(t);
      }
    });
  }

  protected buscar(evento: Event): void {
    evento.preventDefault();
    const t = this.tombo.value.trim();
    if (t) this.procurar(t);
  }

  protected confirmar(evento: Event): void {
    evento.preventDefault();
    const e = this.emprestimo();
    if (!e || this.enviando()) return;
    const oc = this.ocorrencia.value;
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .devolver(e.id, oc === 'NENHUMA' ? null : oc, oc === 'NENHUMA' ? null : this.observacao.value.trim() || null)
      .subscribe({
        next: (r) => {
          this.enviando.set(false);
          this.feitos.update((l) => [{ e: r }, ...l]);
          this.limpar();
        },
        error: (err) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(err));
        },
      });
  }

  protected cancelar(): void {
    this.limpar();
  }

  private procurar(tombo: string): void {
    this.buscando.set(true);
    this.emprestimo.set(null);
    this.erro.set(null);
    this.api.listar({ status: 'ATIVO', tombo, tamanho: 1 }).subscribe({
      next: (p) => {
        this.buscando.set(false);
        if (!p.itens.length) {
          this.marcarErroTombo(`O tombo ${tombo.toUpperCase()} não está emprestado.`);
          return;
        }
        this.ocorrencia.setValue('NENHUMA');
        this.observacao.setValue('');
        this.emprestimo.set(p.itens[0]);
        afterNextRender(() => this.botaoConfirmar()?.nativeElement.focus(), { injector: this.injector });
      },
      error: (err) => {
        this.buscando.set(false);
        this.marcarErroTombo(mensagemDeErro(err));
      },
    });
  }

  private marcarErroTombo(mensagem: string): void {
    this.erroTombo.set(mensagem);
    this.tombo.setErrors({ servidor: true });
    this.tombo.markAsTouched();
    this.campoTombo().nativeElement.select();
  }

  private limpar(): void {
    this.emprestimo.set(null);
    this.tombo.reset('');
    this.campoTombo().nativeElement.focus();
  }
}
