import { Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CampoInvalido, camposComErro, mensagemDeErro } from '../../core/api/erro-api';
import { Obra, ObraRequisicao } from '../../core/api/modelos';
import { AcervoApi } from './acervo-api';
import { DadosFormularioObra, FormularioObra } from './formulario-obra';

@Component({
  selector: 'app-edicao-obra',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, FormularioObra],
  template: `
    <a mat-button [routerLink]="['/painel/acervo', id()]" class="voltar"><mat-icon>arrow_back</mat-icon> Voltar à obra</a>
    <h1>Editar obra</h1>
    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (inicial(); as dados) {
      <app-formulario-obra
        [inicial]="dados"
        [isbnEditavel]="true"
        [salvando]="salvando()"
        [errosServidor]="errosCampos()"
        textoSalvar="Salvar alterações"
        (salvar)="salvar($event)"
        (cancelar)="voltar()"
        (errosSemCampo)="mostrarSobras($event)"
      />
    } @else if (!erro()) {
      <mat-spinner diameter="32" />
    }
  `,
  styles: `
    .voltar {
      margin: 0 0 12px -12px;
    }
    h1 {
      margin-bottom: 16px;
    }
    .aviso {
      margin: 0 0 16px;
      max-width: 640px;
    }
  `,
})
export class EdicaoObra {
  readonly id = input.required({ transform: numberAttribute });

  private readonly api = inject(AcervoApi);
  private readonly router = inject(Router);

  private readonly obra = signal<Obra | null>(null);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly errosCampos = signal<CampoInvalido[]>([]);

  protected readonly inicial = computed<DadosFormularioObra | null>(() => {
    const o = this.obra();
    return o ? { ...o, isbn: o.isbn13 } : null;
  });

  constructor() {
    effect(() => {
      this.api.obra(this.id()).subscribe({
        next: (o) => this.obra.set(o),
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
    });
  }

  protected salvar(req: ObraRequisicao): void {
    this.salvando.set(true);
    this.erro.set(null);
    this.errosCampos.set([]);
    this.api.alterarObra(this.id(), req).subscribe({
      next: () => {
        this.salvando.set(false);
        this.voltar();
      },
      error: (e) => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e));
        this.errosCampos.set(camposComErro(e));
      },
    });
  }

  protected mostrarSobras(mensagens: string[]): void {
    if (mensagens.length) {
      this.erro.set(mensagens.join(' '));
    }
  }

  protected voltar(): void {
    this.router.navigate(['/painel/acervo', this.id()]);
  }
}
