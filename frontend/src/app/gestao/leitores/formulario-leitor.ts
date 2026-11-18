import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { camposComErro, mensagemDeErro } from '../../core/api/erro-api';
import { LeitorRequisicao } from '../../core/api/modelos';
import { cpfValido, formatarCpf } from '../../shared/formatos';
import { aplicarErrosDoServidor, hojeIso, mensagemDoCampo, textoOuNulo } from '../../shared/formularios';
import { LeitoresApi } from './leitores-api';

function validarCpf(c: AbstractControl): ValidationErrors | null {
  return !c.value || cpfValido(c.value) ? null : { cpf: true };
}

function idade(nascimento: string): number {
  const [a, m, d] = nascimento.split('-').map(Number);
  const hoje = new Date();
  let anos = hoje.getFullYear() - a;
  if (hoje.getMonth() + 1 < m || (hoje.getMonth() + 1 === m && hoje.getDate() < d)) {
    anos--;
  }
  return anos;
}

/** Cadastro (`/novo`) e edição (`/:id/editar`) de leitor, em uma coluna. */
@Component({
  selector: 'app-formulario-leitor',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <a mat-button [routerLink]="voltarPara()" class="voltar"><mat-icon>arrow_back</mat-icon> Voltar</a>
    <h1>{{ pendente() ? 'Conferir cadastro' : edicao() ? 'Editar leitor' : 'Cadastrar leitor' }}</h1>

    @if (pendente()) {
      <p class="aviso alerta" role="status">
        <mat-icon>how_to_reg</mat-icon>
        <span>
          O leitor preencheu estes dados no site, ao reservar. Confira nome, CPF e nascimento com um documento com
          foto, complete endereço e responsável (se for menor) e confirme.
        </span>
      </p>
    }

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    <form class="coluna" [formGroup]="form" (ngSubmit)="salvar()">
      <mat-form-field appearance="outline">
        <mat-label>Nome completo</mat-label>
        <input matInput formControlName="nome" autocomplete="off" />
        <mat-error>{{ msg('nome') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>CPF</mat-label>
        <input matInput formControlName="cpf" inputmode="numeric" autocomplete="off" (blur)="formatarCpfDigitado()" />
        <mat-hint>Também é o acesso do leitor à reserva pelo site.</mat-hint>
        <mat-error>{{ msg('cpf') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Data de nascimento</mat-label>
        <input matInput type="date" formControlName="dataNascimento" [max]="hoje" />
        <mat-error>{{ msg('dataNascimento') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Responsável</mat-label>
        <input matInput formControlName="responsavel" />
        <mat-hint>{{ menor() ? 'Obrigatório: o leitor é menor de idade.' : 'Para menores de 18 anos.' }}</mat-hint>
        <mat-error>{{ msg('responsavel') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Telefone</mat-label>
        <input matInput formControlName="telefone" inputmode="tel" />
        <mat-error>{{ msg('telefone') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>E-mail</mat-label>
        <input matInput formControlName="email" type="email" />
        <mat-error>{{ msg('email') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Endereço</mat-label>
        <input matInput formControlName="endereco" />
        <mat-error>{{ msg('endereco') }}</mat-error>
      </mat-form-field>
      @if (edicao()) {
        <mat-checkbox formControlName="ativo">Leitor ativo</mat-checkbox>
      }
      <div class="acoes">
        <button mat-flat-button type="submit" [disabled]="salvando()">
          @if (salvando()) {
            <mat-spinner diameter="18" />
          }
          {{ pendente() ? 'Confirmar cadastro' : 'Salvar' }}
        </button>
        <a mat-button [routerLink]="voltarPara()">Cancelar</a>
      </div>
    </form>
  `,
  styles: `
    .voltar {
      margin: 0 0 12px -12px;
    }
    h1 {
      margin-bottom: 16px;
    }
    .aviso {
      max-width: 640px;
      margin: 0 0 16px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class FormularioLeitor {
  /** Ausente em `/novo`. */
  readonly id = input<string>();

  private readonly api = inject(LeitoresApi);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly hoje = hojeIso();
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());
  /** Pré-cadastro do site: salvar confirma que a equipe conferiu o documento. */
  protected readonly pendente = signal(false);
  protected readonly voltarPara = computed(() => (this.id() ? ['/painel/leitores', this.id()] : ['/painel/leitores']));
  private readonly nascimento = signal('');
  protected readonly menor = computed(() => !!this.nascimento() && idade(this.nascimento()) < 18);

  protected readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    cpf: ['', [Validators.required, validarCpf]],
    dataNascimento: ['', [Validators.required]],
    responsavel: ['', [Validators.maxLength(200)]],
    telefone: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    endereco: ['', [Validators.maxLength(255)]],
    ativo: [true],
  });

  constructor() {
    this.form.controls.dataNascimento.valueChanges.subscribe((v) => this.nascimento.set(v));
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.buscar(Number(id)).subscribe({
        next: (l) => {
          this.pendente.set(l.cadastroPendente);
          this.form.reset({
            nome: l.nome,
            cpf: formatarCpf(l.cpf),
            dataNascimento: l.dataNascimento,
            responsavel: l.responsavel ?? '',
            telefone: l.telefone ?? '',
            email: l.email ?? '',
            endereco: l.endereco ?? '',
            ativo: l.ativo,
          });
        },
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
    });
  }

  protected msg(campo: string): string {
    const c = this.form.get(campo);
    if (c?.errors?.['cpf']) return 'CPF inválido. Confira os números.';
    if (c?.errors?.['email']) return 'E-mail inválido.';
    if (c?.errors?.['responsavelObrigatorio']) return 'Leitor menor de idade: informe o responsável.';
    return mensagemDoCampo(c);
  }

  protected formatarCpfDigitado(): void {
    const c = this.form.controls.cpf;
    if (cpfValido(c.value)) {
      c.setValue(formatarCpf(c.value));
    }
  }

  protected salvar(): void {
    const resp = this.form.controls.responsavel;
    if (this.menor() && !resp.value.trim()) {
      resp.setErrors({ responsavelObrigatorio: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: LeitorRequisicao = {
      nome: v.nome.trim(),
      cpf: v.cpf,
      dataNascimento: v.dataNascimento,
      responsavel: textoOuNulo(v.responsavel),
      telefone: textoOuNulo(v.telefone),
      email: textoOuNulo(v.email),
      endereco: textoOuNulo(v.endereco),
      ...(this.edicao() ? { ativo: v.ativo } : {}),
    };
    this.salvando.set(true);
    this.erro.set(null);
    const id = this.id();
    const pedido = id ? this.api.alterar(Number(id), req) : this.api.cadastrar(req);
    pedido.subscribe({
      next: (l) => {
        this.salvando.set(false);
        this.router.navigate(['/painel/leitores', l.id]);
      },
      error: (e) => {
        this.salvando.set(false);
        const campos = camposComErro(e);
        const sobras = aplicarErrosDoServidor(this.form, campos);
        this.erro.set(sobras.length || !campos.length ? mensagemDeErro(e) : null);
      },
    });
  }
}
