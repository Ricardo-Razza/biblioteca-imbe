import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Sessao } from '../../core/auth/sessao';

/** Mesmas regras do backend: 8 a 72 caracteres, com letra e número. */
export function senhaForte(c: AbstractControl<string>): ValidationErrors | null {
  const v = c.value ?? '';
  if (!v) return null;
  if (v.length < 8) return { senha: 'Use pelo menos 8 caracteres.' };
  if (v.length > 72) return { senha: 'Use no máximo 72 caracteres.' };
  if (!/\p{L}/u.test(v) || !/\d/.test(v)) return { senha: 'Use letras e números.' };
  return null;
}

/** Troca da própria senha (usuário local). Obrigatória com senha provisória. */
@Component({
  selector: 'app-troca-senha',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule],
  template: `
    <h1>Trocar senha</h1>
    @if (provisoria()) {
      <p class="aviso alerta" role="status">
        <mat-icon>key</mat-icon>
        <span>Você entrou com uma senha provisória. Escolha a sua senha para continuar.</span>
      </p>
    }
    @if (sessao.funcionario()?.tipo === 'AD') {
      <p class="aviso alerta" role="status">
        <mat-icon>info</mat-icon>
        <span>Você entra com o usuário da rede da Prefeitura. A senha é trocada no sistema da Prefeitura.</span>
      </p>
    } @else {
      @if (feito()) {
        <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> Senha trocada.</p>
      }
      @if (erro()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
      }
      <form class="coluna" [formGroup]="form" (ngSubmit)="salvar()">
        <mat-form-field appearance="outline">
          <mat-label>Senha atual</mat-label>
          <input matInput type="password" formControlName="atual" autocomplete="current-password" />
          @if (provisoria()) {
            <mat-hint>A senha provisória que você recebeu.</mat-hint>
          }
          <mat-error>Informe a senha atual.</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nova senha</mat-label>
          <input matInput type="password" formControlName="nova" autocomplete="new-password" />
          <mat-hint>Pelo menos 8 caracteres, com letras e números.</mat-hint>
          <mat-error>{{ erroNova() }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Repita a nova senha</mat-label>
          <input matInput type="password" formControlName="repetida" autocomplete="new-password" />
          <mat-error>As senhas não são iguais.</mat-error>
        </mat-form-field>
        <div class="acoes">
          <button mat-flat-button type="submit" [disabled]="enviando()">
            @if (enviando()) {
              <mat-spinner diameter="18" />
            }
            Trocar senha
          </button>
          @if (provisoria()) {
            <button mat-button type="button" (click)="sair()">Sair</button>
          }
        </div>
      </form>
    }
  `,
  styles: `
    h1 {
      margin-bottom: 16px;
    }
    .aviso {
      max-width: 560px;
    }
    form {
      max-width: 420px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class TrocaSenha {
  protected readonly sessao = inject(Sessao);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    atual: new FormControl('', { nonNullable: true, validators: Validators.required }),
    nova: new FormControl('', { nonNullable: true, validators: [Validators.required, senhaForte] }),
    repetida: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });
  protected readonly provisoria = computed(() => !!this.sessao.funcionario()?.trocarSenha);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly feito = signal(false);

  protected erroNova(): string {
    const e = this.form.controls.nova.errors;
    return e?.['senha'] ?? 'Informe a nova senha.';
  }

  protected salvar(): void {
    const { atual, nova, repetida } = this.form.getRawValue();
    if (nova && repetida && nova !== repetida) {
      this.form.controls.repetida.setErrors({ diferente: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const eraProvisoria = this.provisoria();
    this.enviando.set(true);
    this.erro.set(null);
    this.feito.set(false);
    this.sessao.trocarSenha(atual, nova).subscribe({
      next: () => {
        this.enviando.set(false);
        this.form.reset();
        if (eraProvisoria) {
          this.router.navigateByUrl('/painel');
        } else {
          this.feito.set(true);
        }
      },
      error: (e) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected sair(): void {
    this.sessao.sair();
    this.router.navigateByUrl('/login');
  }
}
