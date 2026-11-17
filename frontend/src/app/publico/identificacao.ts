import { Component, input, output } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { cpfValido, formatarCpf } from '../shared/formatos';

export interface DadosIdentificacao {
  cpf: string;
  dataNascimento: string;
}

function validarCpf(c: AbstractControl): ValidationErrors | null {
  return !c.value || cpfValido(c.value) ? null : { cpf: true };
}

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * CPF e data de nascimento do leitor, em uma coluna. Os dados ficam só na memória da página: nada vai para a URL
 * nem para o armazenamento do navegador.
 */
@Component({
  selector: 'app-identificacao',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form class="coluna" [formGroup]="form" (ngSubmit)="enviar()">
      <mat-form-field appearance="outline">
        <mat-label>CPF</mat-label>
        <input matInput formControlName="cpf" inputmode="numeric" autocomplete="off" placeholder="000.000.000-00" (blur)="formatar()" />
        <mat-error>{{ form.controls.cpf.hasError('cpf') ? 'CPF inválido. Confira os números.' : 'Informe o CPF.' }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Data de nascimento</mat-label>
        <input matInput type="date" formControlName="dataNascimento" [max]="hoje" autocomplete="bday" />
        <mat-error>Informe a data de nascimento.</mat-error>
      </mat-form-field>
      <div class="acoes">
        <button mat-flat-button type="submit" [disabled]="ocupado()">{{ textoBotao() }}</button>
      </div>
    </form>
  `,
  styles: `
    .coluna {
      max-width: 420px;
    }
    button[type='submit'] {
      min-width: 160px;
      height: 48px;
    }
  `,
})
export class Identificacao {
  readonly textoBotao = input('Continuar');
  readonly ocupado = input(false);
  readonly enviado = output<DadosIdentificacao>();

  protected readonly hoje = hojeIso();
  protected readonly form = new FormGroup({
    cpf: new FormControl('', { nonNullable: true, validators: [Validators.required, validarCpf] }),
    dataNascimento: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });

  protected formatar(): void {
    const c = this.form.controls.cpf;
    if (cpfValido(c.value)) {
      c.setValue(formatarCpf(c.value));
    }
  }

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { cpf, dataNascimento } = this.form.getRawValue();
    this.enviado.emit({ cpf: cpf.replace(/\D/g, ''), dataNascimento });
  }
}
