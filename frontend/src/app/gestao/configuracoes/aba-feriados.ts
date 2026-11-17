import { Component, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { mensagemDeErro } from '../../core/api/erro-api';
import { dataBr } from '../../shared/formatos';
import { mensagemDoCampo } from '../../shared/formularios';
import { Selo } from '../../shared/selo/selo';
import { ConfiguracoesApi, Feriado } from './configuracoes-api';

const DIAS = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado'];

/** Feriados do ano. Os nacionais vêm da BrasilAPI; aqui se incluem os municipais e pontos sem atendimento. */
@Component({
  selector: 'app-aba-feriados',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, Selo],
  template: `
    <p class="texto-secundario">
      Dias sem atendimento além de sábado e domingo. Prazos que caem nesses dias passam para o próximo dia útil.
    </p>

    <div class="ano">
      <button mat-icon-button type="button" (click)="ano.set(ano() - 1)" aria-label="Ano anterior"><mat-icon>chevron_left</mat-icon></button>
      <h2>{{ ano() }}</h2>
      <button mat-icon-button type="button" (click)="ano.set(ano() + 1)" aria-label="Próximo ano"><mat-icon>chevron_right</mat-icon></button>
    </div>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (removendo(); as f) {
      <div class="aviso alerta" role="alertdialog" aria-label="Confirmar remoção">
        <mat-icon>help</mat-icon>
        <span>Remover {{ f.descricao }} ({{ data(f.data) }})? Prazos já calculados não mudam.</span>
        <button mat-button type="button" (click)="removendo.set(null)">Cancelar</button>
        <button mat-flat-button type="button" (click)="remover(f)">Remover</button>
      </div>
    }

    @if (feriados().length) {
      <ul class="lista">
        @for (f of feriados(); track f.data) {
          <li>
            <span class="data">{{ data(f.data) }}</span>
            <span class="dia texto-secundario">{{ diaDaSemana(f.data) }}</span>
            <span class="descricao">{{ f.descricao }}</span>
            <app-selo [tom]="f.tipo === 'MUNICIPAL' ? 'alerta' : 'neutra'">{{ f.tipo === 'MUNICIPAL' ? 'Municipal' : 'Nacional' }}</app-selo>
            <button mat-icon-button type="button" (click)="removendo.set(f)" [attr.aria-label]="'Remover ' + f.descricao">
              <mat-icon>delete</mat-icon>
            </button>
          </li>
        }
      </ul>
    } @else {
      <p class="texto-secundario">Nenhum feriado cadastrado em {{ ano() }}.</p>
    }

    <section class="painel" aria-labelledby="titulo-novo-feriado">
      <h2 id="titulo-novo-feriado">Incluir feriado municipal</h2>
      <form class="coluna" [formGroup]="form" (ngSubmit)="incluir()">
        <mat-form-field appearance="outline">
          <mat-label>Data</mat-label>
          <input matInput type="date" formControlName="data" />
          <mat-error>{{ msg(form.controls.data) }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nome</mat-label>
          <input matInput formControlName="descricao" maxlength="150" />
          <mat-hint>Ex.: Aniversário de Imbé, ponto facultativo.</mat-hint>
          <mat-error>{{ msg(form.controls.descricao) }}</mat-error>
        </mat-form-field>
        <div class="acoes">
          <button mat-flat-button type="submit" [disabled]="ocupado()">Incluir</button>
        </div>
      </form>
    </section>
  `,
  styles: `
    :host {
      display: block;
      padding-top: 8px;
    }
    .ano {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 8px 0;
    }
    .ano h2 {
      margin: 0;
      min-width: 56px;
      text-align: center;
    }
    .lista {
      list-style: none;
      padding: 0;
      margin: 0 0 24px;
      max-width: 760px;
    }
    .lista li {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 6px 4px 6px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .data {
      font-variant-numeric: tabular-nums;
      width: 92px;
    }
    .dia {
      width: 64px;
    }
    .descricao {
      flex: 1;
    }
    .painel {
      max-width: 480px;
    }
    .aviso {
      max-width: 760px;
    }
  `,
})
export class AbaFeriados {
  private readonly api = inject(ConfiguracoesApi);
  protected readonly ano = signal(new Date().getFullYear());
  protected readonly feriados = signal<Feriado[]>([]);
  protected readonly removendo = signal<Feriado | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly data = dataBr;
  protected readonly msg = mensagemDoCampo;
  protected readonly form = new FormGroup({
    data: new FormControl('', { nonNullable: true, validators: Validators.required }),
    descricao: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
  });

  constructor() {
    effect(() => this.carregar(this.ano()));
  }

  protected diaDaSemana(iso: string): string {
    const [a, m, d] = iso.split('-').map(Number);
    return DIAS[new Date(a, m - 1, d).getDay()];
  }

  protected incluir(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { data, descricao } = this.form.getRawValue();
    this.ocupado.set(true);
    this.erro.set(null);
    this.api.incluirFeriado(data, descricao).subscribe({
      next: (f) => {
        this.ocupado.set(false);
        this.form.reset();
        const ano = Number(f.data.slice(0, 4));
        if (ano === this.ano()) this.carregar(ano);
        else this.ano.set(ano);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected remover(f: Feriado): void {
    this.removendo.set(null);
    this.api.removerFeriado(f.data).subscribe({
      next: () => this.carregar(this.ano()),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private carregar(ano: number): void {
    this.api.feriados(ano).subscribe({
      next: (l) => this.feriados.set(l),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
