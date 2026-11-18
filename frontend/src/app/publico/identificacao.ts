import { Component, input, output } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { cpfValido, formatarCpf } from '../shared/formatos';

export interface DadosIdentificacao {
  cpf: string;
  dataNascimento: string;
  /** Só quando o formulário pede contato (reserva). Nome serve para o pré-cadastro de quem ainda não é leitor. */
  nome?: string;
  telefone?: string;
  email?: string | null;
}

function validarCpf(c: AbstractControl): ValidationErrors | null {
  return !c.value || cpfValido(c.value) ? null : { cpf: true };
}

/** Dígitos do telefone com DDD (10 ou 11), sem o 55 do país. Nulo se não for um telefone válido. */
export function digitosTelefone(valor: string): string | null {
  let d = (valor ?? '').replace(/\D/g, '');
  if ((d.length === 12 || d.length === 13) && d.startsWith('55')) {
    d = d.slice(2);
  }
  return d.length === 10 || d.length === 11 ? d : null;
}

/** (51) 99999-9999 ou (51) 3333-4444. */
export function formatarTelefone(valor: string): string {
  const d = digitosTelefone(valor);
  if (!d) {
    return valor;
  }
  const meio = d.length - 4;
  return `(${d.slice(0, 2)}) ${d.slice(2, meio)}-${d.slice(meio)}`;
}

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * CPF e data de nascimento do leitor, em uma coluna; com {@link pedirContato}, também telefone (obrigatório) e
 * e-mail (opcional) para a biblioteca avisar sobre a reserva. Os dados ficam só na memória da página: nada vai
 * para a URL nem para o armazenamento do navegador.
 */
@Component({
  selector: 'app-identificacao',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form class="coluna" [formGroup]="form" (ngSubmit)="enviar()">
      @if (pedirContato()) {
        <mat-form-field appearance="outline">
          <mat-label>Nome completo</mat-label>
          <input matInput formControlName="nome" required autocomplete="name" />
          <mat-error>Informe o nome completo.</mat-error>
        </mat-form-field>
      }
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
      @if (pedirContato()) {
        <mat-form-field appearance="outline">
          <mat-label>Telefone</mat-label>
          <input matInput formControlName="telefone" required type="tel" inputmode="tel" autocomplete="tel" placeholder="(51) 99999-9999" (blur)="formatarFone()" />
          <mat-hint>Com DDD. A biblioteca usa só para falar sobre a reserva.</mat-hint>
          <mat-error>{{ form.controls.telefone.hasError('telefone') ? 'Telefone inválido. Use o DDD, ex.: (51) 99999-9999.' : 'Informe um telefone para contato.' }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>E-mail (opcional)</mat-label>
          <input matInput formControlName="email" type="email" autocomplete="email" />
          <mat-error>E-mail inválido. Confira o endereço.</mat-error>
        </mat-form-field>
      }
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
  /** Reserva: pede telefone e e-mail. "Minhas reservas" não pede. */
  readonly pedirContato = input(false);
  readonly enviado = output<DadosIdentificacao>();

  protected readonly hoje = hojeIso();
  protected readonly form = new FormGroup({
    nome: new FormControl('', {
      nonNullable: true,
      validators: [(c) => (this.pedirContato() && !c.value?.trim() ? { required: true } : null), Validators.maxLength(200)],
    }),
    cpf: new FormControl('', { nonNullable: true, validators: [Validators.required, validarCpf] }),
    dataNascimento: new FormControl('', { nonNullable: true, validators: Validators.required }),
    telefone: new FormControl('', { nonNullable: true, validators: (c) => this.validarTelefone(c) }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(150)] }),
  });

  private validarTelefone(c: AbstractControl): ValidationErrors | null {
    if (!this.pedirContato()) {
      return null;
    }
    if (!c.value?.trim()) {
      return { required: true };
    }
    return digitosTelefone(c.value) ? null : { telefone: true };
  }

  protected formatar(): void {
    const c = this.form.controls.cpf;
    if (cpfValido(c.value)) {
      c.setValue(formatarCpf(c.value));
    }
  }

  protected formatarFone(): void {
    const c = this.form.controls.telefone;
    c.setValue(formatarTelefone(c.value));
  }

  protected enviar(): void {
    // Os validadores de nome e telefone dependem do input pedirContato: recalcula antes de conferir.
    this.form.controls.nome.updateValueAndValidity();
    this.form.controls.telefone.updateValueAndValidity();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, cpf, dataNascimento, telefone, email } = this.form.getRawValue();
    const dados: DadosIdentificacao = { cpf: cpf.replace(/\D/g, ''), dataNascimento };
    if (this.pedirContato()) {
      dados.nome = nome.trim().replace(/\s+/g, ' ');
      dados.telefone = formatarTelefone(telefone.trim());
      dados.email = email.trim() || null;
    }
    this.enviado.emit(dados);
  }
}
