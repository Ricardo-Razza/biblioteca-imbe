import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { mensagemDeErro } from '../../core/api/erro-api';
import { Sessao } from '../../core/auth/sessao';

/** Login da equipe: usuário de rede (AD) ou local, no mesmo formulário. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule],
  template: `
    <main class="fundo">
      <section class="cartao" aria-labelledby="titulo-login">
        <p class="marca">Biblioteca Municipal de Imbé</p>
        <h1 id="titulo-login">Entrar na gestão</h1>

        @if (expirou() && !erro()) {
          <p class="aviso alerta" role="status"><mat-icon>schedule</mat-icon> Sua sessão terminou. Entre de novo.</p>
        }
        @if (erro()) {
          <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
        }

        <form class="coluna" [formGroup]="form" (ngSubmit)="entrar()">
          <mat-form-field appearance="outline">
            <mat-label>Login</mat-label>
            <input matInput formControlName="login" autocomplete="username" autocapitalize="none" spellcheck="false" />
            <mat-hint>O mesmo usuário da rede da Prefeitura.</mat-hint>
            <mat-error>Informe o login.</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Senha</mat-label>
            <input matInput [type]="verSenha() ? 'text' : 'password'" formControlName="senha" autocomplete="current-password" />
            <button
              mat-icon-button
              matSuffix
              type="button"
              (click)="verSenha.set(!verSenha())"
              [attr.aria-label]="verSenha() ? 'Esconder senha' : 'Mostrar senha'"
            >
              <mat-icon>{{ verSenha() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            <mat-error>Informe a senha.</mat-error>
          </mat-form-field>
          <button mat-flat-button type="submit" class="entrar" [disabled]="enviando()">
            @if (enviando()) {
              <mat-spinner diameter="18" />
            }
            Entrar
          </button>
        </form>
        <p class="texto-secundario ajuda">Esqueceu a senha? Fale com o administrador do sistema.</p>
      </section>
    </main>
  `,
  styles: `
    .fundo {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 16px;
      background: var(--mat-sys-surface-container-low);
    }
    .cartao {
      width: 100%;
      max-width: 400px;
      background: var(--mat-sys-surface);
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 12px;
      padding: 32px 28px 24px;
    }
    .marca {
      margin: 0;
      color: var(--mat-sys-primary);
      font: var(--mat-sys-title-small);
    }
    h1 {
      margin: 4px 0 20px;
    }
    .entrar {
      height: 48px;
      margin-top: 4px;
    }
    .ajuda {
      margin: 20px 0 0;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class Login {
  readonly volta = input<string>();
  readonly expirou = input<string>();

  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    login: new FormControl('', { nonNullable: true, validators: Validators.required }),
    senha: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly verSenha = signal(false);

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.enviando()) return;
    this.enviando.set(true);
    this.erro.set(null);
    const { login, senha } = this.form.getRawValue();
    this.sessao.entrar(login, senha).subscribe({
      next: (f) => {
        this.enviando.set(false);
        const destino = f.trocarSenha ? '/painel/senha' : this.destinoSeguro();
        this.router.navigateByUrl(destino);
      },
      error: (e) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(e));
        this.form.controls.senha.reset('');
      },
    });
  }

  /** Só volta para dentro da gestão (evita redirecionar para fora a partir da URL). */
  private destinoSeguro(): string {
    const v = this.volta();
    return v && v.startsWith('/painel') ? v : '/painel';
  }
}
