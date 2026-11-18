import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';

import { camposComErro, mensagemDeErro } from '../../core/api/erro-api';
import { Perfil, Sessao, TipoAutenticacao } from '../../core/auth/sessao';
import { aplicarErrosDoServidor, mensagemDoCampo } from '../../shared/formularios';
import { Selo } from '../../shared/selo/selo';
import { ConfiguracoesApi, Funcionario, SenhaProvisoria } from './configuracoes-api';

/** Funcionários: cadastro (rede ou local), edição, desativação e nova senha provisória. */
@Component({
  selector: 'app-aba-funcionarios',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatRadioModule,
    MatSelectModule,
    Selo,
  ],
  template: `
    @if (senhaMostrada(); as s) {
      <div class="aviso ok senha" role="status">
        <mat-icon>key</mat-icon>
        <div>
          <p>Senha provisória de <strong>{{ s.funcionario.nome }}</strong> (login {{ s.funcionario.login }}):</p>
          <p class="provisoria">{{ s.senhaProvisoria }}</p>
          <p>Entregue ao funcionário. Ela não aparece de novo e precisa ser trocada no primeiro acesso.</p>
        </div>
        <button mat-button type="button" (click)="senhaMostrada.set(null)">Fechar</button>
      </div>
    }
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }

    @if (confirmandoSenha(); as f) {
      <div class="aviso alerta" role="alertdialog" aria-label="Confirmar nova senha">
        <mat-icon>help</mat-icon>
        <span>Gerar uma nova senha provisória para {{ f.nome }}? A senha atual deixa de valer.</span>
        <button mat-button type="button" (click)="confirmandoSenha.set(null)">Cancelar</button>
        <button mat-flat-button type="button" (click)="redefinir(f)">Gerar senha</button>
      </div>
    }

    @if (editando(); as f) {
      <section class="painel" aria-labelledby="titulo-edicao">
        <h2 id="titulo-edicao">Editar {{ f.login }}</h2>
        <form class="coluna" [formGroup]="formEdicao" (ngSubmit)="salvarEdicao(f)">
          <mat-form-field appearance="outline">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" maxlength="150" />
            <mat-error>{{ msg(formEdicao.controls.nome) }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Perfil</mat-label>
            <mat-select formControlName="perfil">
              <mat-option value="ATENDENTE">Atendente</mat-option>
              <mat-option value="ADMIN">Administrador</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-checkbox formControlName="ativo">Ativo (pode entrar no sistema)</mat-checkbox>
          <div class="acoes">
            <button mat-flat-button type="submit" [disabled]="ocupado()">Salvar</button>
            <button mat-button type="button" (click)="editando.set(null)">Cancelar</button>
          </div>
        </form>
      </section>
    } @else if (cadastrando()) {
      <section class="painel" aria-labelledby="titulo-cadastro">
        <h2 id="titulo-cadastro">Cadastrar funcionário</h2>
        <form class="coluna" [formGroup]="formCadastro" (ngSubmit)="cadastrar()">
          <mat-form-field appearance="outline">
            <mat-label>Nome completo</mat-label>
            <input matInput formControlName="nome" maxlength="150" />
            <mat-error>{{ msg(formCadastro.controls.nome) }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Login</mat-label>
            <input matInput formControlName="login" maxlength="60" autocapitalize="none" spellcheck="false" />
            <mat-hint>Para usuário de rede, o mesmo login da Prefeitura.</mat-hint>
            <mat-error>{{ msg(formCadastro.controls.login) }}</mat-error>
          </mat-form-field>
          <mat-radio-group formControlName="tipo" class="opcoes" aria-label="Como entra">
            <mat-radio-button value="AD">Usuário da rede da Prefeitura</mat-radio-button>
            <mat-radio-button value="LOCAL">Usuário local (senha do próprio sistema)</mat-radio-button>
          </mat-radio-group>
          <mat-form-field appearance="outline">
            <mat-label>Perfil</mat-label>
            <mat-select formControlName="perfil">
              <mat-option value="ATENDENTE">Atendente</mat-option>
              <mat-option value="ADMIN">Administrador</mat-option>
            </mat-select>
            <mat-hint>Administrador também cuida de configurações, baixas e desbloqueios.</mat-hint>
          </mat-form-field>
          <div class="acoes">
            <button mat-flat-button type="submit" [disabled]="ocupado()">Cadastrar</button>
            <button mat-button type="button" (click)="cadastrando.set(false)">Cancelar</button>
          </div>
        </form>
      </section>
    } @else {
      <div class="acoes topo">
        <button mat-flat-button type="button" (click)="abrirCadastro()"><mat-icon>person_add</mat-icon> Cadastrar funcionário</button>
      </div>
    }

    <ul class="lista">
      @for (f of funcionarios(); track f.id) {
        <li [class.inativo]="!f.ativo">
          <div class="info">
            <span class="nome">{{ f.nome }}</span>
            <span class="texto-secundario">
              {{ f.login }} · {{ f.tipo === 'AD' ? 'rede' : 'local' }} ·
              {{ f.ultimoAcesso ? 'último acesso em ' + (f.ultimoAcesso | date: 'dd/MM/yyyy HH:mm') : 'nunca entrou' }}
            </span>
          </div>
          <div class="selos">
            <app-selo [tom]="f.perfil === 'ADMIN' ? 'alerta' : 'neutra'">{{ f.perfil === 'ADMIN' ? 'Administrador' : 'Atendente' }}</app-selo>
            @if (!f.ativo) {
              <app-selo tom="neutra">Desativado</app-selo>
            }
            @if (f.trocarSenha && f.ativo) {
              <app-selo tom="alerta">Senha provisória</app-selo>
            }
            @if (f.loginTravadoAte) {
              <app-selo tom="erro">Travado até {{ f.loginTravadoAte | date: 'HH:mm' }}</app-selo>
            }
          </div>
          <button mat-icon-button type="button" [matMenuTriggerFor]="menu" [attr.aria-label]="'Ações de ' + f.nome">
            <mat-icon>more_vert</mat-icon>
          </button>
          <mat-menu #menu="matMenu">
            <button mat-menu-item type="button" (click)="abrirEdicao(f)"><mat-icon>edit</mat-icon> Editar</button>
            @if (f.tipo === 'LOCAL') {
              <button mat-menu-item type="button" (click)="confirmandoSenha.set(f)"><mat-icon>key</mat-icon> Nova senha provisória</button>
            }
          </mat-menu>
        </li>
      }
    </ul>
  `,
  styles: `
    :host {
      display: block;
      padding-top: 16px;
    }
    .topo {
      margin-bottom: 8px;
    }
    .painel {
      max-width: 560px;
      padding: 8px 16px 16px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      margin-bottom: 16px;
    }
    .opcoes {
      display: flex;
      flex-direction: column;
      margin-bottom: 8px;
    }
    .aviso {
      max-width: 760px;
      margin: 0 0 12px;
    }
    .senha p {
      margin: 0 0 4px;
    }
    .provisoria {
      font: var(--mat-sys-title-large);
      font-family: ui-monospace, Consolas, monospace;
      letter-spacing: 2px;
      user-select: all;
    }
    .lista {
      list-style: none;
      margin: 0;
      padding: 0;
      max-width: 900px;
    }
    .lista li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      padding: 10px 4px 10px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .inativo .nome {
      color: var(--mat-sys-on-surface-variant);
    }
    .info {
      flex: 1 1 200px;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .nome {
      font-weight: 500;
    }
    .selos {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      justify-content: flex-end;
    }
  `,
})
export class AbaFuncionarios implements OnInit {
  private readonly api = inject(ConfiguracoesApi);
  private readonly sessao = inject(Sessao);

  protected readonly funcionarios = signal<Funcionario[]>([]);
  protected readonly cadastrando = signal(false);
  protected readonly editando = signal<Funcionario | null>(null);
  protected readonly confirmandoSenha = signal<Funcionario | null>(null);
  protected readonly senhaMostrada = signal<SenhaProvisoria | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly msg = mensagemDoCampo;

  protected readonly formCadastro = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    login: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\s*[A-Za-z0-9._-]{3,60}\s*$/)] }),
    tipo: new FormControl<TipoAutenticacao>('AD', { nonNullable: true }),
    perfil: new FormControl<Perfil>('ATENDENTE', { nonNullable: true }),
  });
  protected readonly formEdicao = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    perfil: new FormControl<Perfil>('ATENDENTE', { nonNullable: true }),
    ativo: new FormControl(true, { nonNullable: true }),
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected abrirCadastro(): void {
    this.limparAvisos();
    this.formCadastro.reset();
    this.cadastrando.set(true);
  }

  protected abrirEdicao(f: Funcionario): void {
    this.limparAvisos();
    this.cadastrando.set(false);
    this.formEdicao.reset({ nome: f.nome, perfil: f.perfil, ativo: f.ativo });
    const euMesmo = f.id === this.sessao.funcionario()?.id;
    if (euMesmo) {
      this.formEdicao.controls.perfil.disable();
      this.formEdicao.controls.ativo.disable();
    } else {
      this.formEdicao.controls.perfil.enable();
      this.formEdicao.controls.ativo.enable();
    }
    this.editando.set(f);
  }

  protected cadastrar(): void {
    if (this.formCadastro.invalid) {
      this.formCadastro.markAllAsTouched();
      return;
    }
    this.ocupado.set(true);
    this.limparAvisos();
    this.api.cadastrarFuncionario(this.formCadastro.getRawValue()).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.cadastrando.set(false);
        if (r.senhaProvisoria) {
          this.senhaMostrada.set(r);
        } else {
          this.mensagem.set(`${r.funcionario.nome} cadastrado. Entra com o usuário e a senha da rede.`);
        }
        this.carregar();
      },
      error: (e) => this.falhou(e, this.formCadastro),
    });
  }

  protected salvarEdicao(f: Funcionario): void {
    if (this.formEdicao.invalid) {
      this.formEdicao.markAllAsTouched();
      return;
    }
    this.ocupado.set(true);
    this.limparAvisos();
    this.api.alterarFuncionario(f.id, this.formEdicao.getRawValue()).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.editando.set(null);
        this.mensagem.set(`Dados de ${r.nome} salvos.`);
        this.carregar();
      },
      error: (e) => this.falhou(e, this.formEdicao),
    });
  }

  protected redefinir(f: Funcionario): void {
    this.confirmandoSenha.set(null);
    this.limparAvisos();
    this.api.redefinirSenha(f.id).subscribe({
      next: (r) => {
        this.senhaMostrada.set(r);
        this.carregar();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private carregar(): void {
    this.api.funcionarios().subscribe({
      next: (l) => this.funcionarios.set(l),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private falhou(e: unknown, form: FormGroup): void {
    this.ocupado.set(false);
    const sobras = aplicarErrosDoServidor(form, camposComErro(e));
    if (!camposComErro(e).length || sobras.length) {
      this.erro.set(sobras[0] ?? mensagemDeErro(e));
    }
  }

  private limparAvisos(): void {
    this.erro.set(null);
    this.mensagem.set(null);
    this.senhaMostrada.set(null);
  }
}
