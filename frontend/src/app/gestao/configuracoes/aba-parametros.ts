import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { concatMap, from, toArray } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Parametro } from '../../core/api/modelos';
import { mensagemDoCampo } from '../../shared/formularios';
import { ConfiguracoesApi } from './configuracoes-api';

/** Prazos e limites da tabela `parametro`. Só os alterados são enviados. */
@Component({
  selector: 'app-aba-parametros',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <p class="texto-secundario">Valem para os próximos empréstimos, renovações e reservas. Os já feitos não mudam.</p>
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (parametros().length) {
      <form class="coluna" [formGroup]="form" (ngSubmit)="salvar()">
        @for (p of parametros(); track p.chave) {
          <div class="parametro">
            <label class="descricao" [for]="'param-' + p.chave">{{ p.descricao }}</label>
            <mat-form-field appearance="outline">
              <input matInput type="number" [id]="'param-' + p.chave" [formControlName]="p.chave" [min]="p.minimo" [max]="p.maximo" />
              <mat-hint>De {{ p.minimo }} a {{ p.maximo }}.</mat-hint>
              <mat-error>{{ msg(form.get(p.chave)) }}</mat-error>
            </mat-form-field>
          </div>
        }
        <div class="acoes">
          <button mat-flat-button type="submit" [disabled]="ocupado() || form.pristine">Salvar</button>
        </div>
      </form>
    }
  `,
  styles: `
    :host {
      display: block;
      padding-top: 8px;
    }
    form {
      max-width: 480px;
    }
    /* A descrição vem do banco e é longa: fica acima do campo, quebrando linha, em vez de rótulo cortado. */
    .parametro {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .descricao {
      font: var(--mat-sys-body-large);
      color: var(--mat-sys-on-surface);
    }
    .aviso {
      max-width: 640px;
    }
  `,
})
export class AbaParametros implements OnInit {
  private readonly api = inject(ConfiguracoesApi);
  protected readonly parametros = signal<Parametro[]>([]);
  protected readonly form = new FormGroup<Record<string, FormControl<number>>>({});
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly msg = mensagemDoCampo;

  ngOnInit(): void {
    this.carregar();
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const alterados = this.parametros().filter((p) => this.form.controls[p.chave].value !== p.valor);
    if (!alterados.length) return;
    this.ocupado.set(true);
    this.erro.set(null);
    this.mensagem.set(null);
    from(alterados)
      .pipe(
        concatMap((p) => this.api.alterarParametro(p.chave, this.form.controls[p.chave].value)),
        toArray(),
      )
      .subscribe({
        next: (l) => {
          this.ocupado.set(false);
          this.mensagem.set(l.length === 1 ? 'Valor salvo.' : `${l.length} valores salvos.`);
          this.carregar();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.erro.set(mensagemDeErro(e));
          this.carregar();
        },
      });
  }

  private carregar(): void {
    this.api.parametros().subscribe({
      next: (l) => {
        for (const p of l) {
          const validadores = [Validators.required, Validators.min(p.minimo), Validators.max(p.maximo)];
          const c = this.form.controls[p.chave];
          if (c) {
            c.setValue(p.valor);
          } else {
            this.form.addControl(p.chave, new FormControl(p.valor, { nonNullable: true, validators: validadores }));
          }
        }
        this.form.markAsPristine();
        this.parametros.set(l);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
