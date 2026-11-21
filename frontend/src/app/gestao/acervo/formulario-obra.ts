import { Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CampoInvalido } from '../../core/api/erro-api';
import { FonteIsbn, ObraRequisicao } from '../../core/api/modelos';
import { aplicarErrosDoServidor, linhas, mensagemDoCampo, textoOuNulo } from '../../shared/formularios';

/** Valores para abrir o formulário. Listas viram uma linha por item. */
export interface DadosFormularioObra {
  isbn: string;
  titulo?: string | null;
  subtitulo?: string | null;
  autores?: string[];
  editora?: string | null;
  ano?: number | null;
  paginas?: number | null;
  idioma?: string | null;
  sinopse?: string | null;
  assuntos?: string[];
  fonteDados?: FonteIsbn | null;
}

/** Formulário de obra (cadastro e edição), em uma coluna, com as mesmas regras do backend. */
@Component({
  selector: 'app-formulario-obra',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  template: `
    <form class="coluna" [formGroup]="form" (ngSubmit)="enviar()">
      <mat-form-field appearance="outline">
        <mat-label>ISBN</mat-label>
        <input matInput formControlName="isbn" [readonly]="!isbnEditavel()" autocomplete="off" />
        @if (!isbnEditavel()) {
          <mat-hint>Para outro ISBN, volte e bipe de novo.</mat-hint>
        }
        <mat-error>{{ erro('isbn') }}</mat-error>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Título</mat-label>
        <input matInput formControlName="titulo" />
        <mat-error>{{ erro('titulo') }}</mat-error>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Subtítulo</mat-label>
        <input matInput formControlName="subtitulo" />
        <mat-error>{{ erro('subtitulo') }}</mat-error>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Autores</mat-label>
        <textarea matInput formControlName="autores" rows="2"></textarea>
        <mat-hint>Um por linha, na ordem da capa.</mat-hint>
        <mat-error>{{ erro('autores') }}</mat-error>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Editora</mat-label>
        <input matInput formControlName="editora" />
        <mat-error>{{ erro('editora') }}</mat-error>
      </mat-form-field>

      <div class="linha">
        <mat-form-field appearance="outline">
          <mat-label>Ano</mat-label>
          <input matInput type="number" formControlName="ano" />
          <mat-error>{{ erro('ano') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Páginas</mat-label>
          <input matInput type="number" formControlName="paginas" />
          <mat-error>{{ erro('paginas') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Idioma</mat-label>
          <input matInput formControlName="idioma" />
          <mat-hint>pt, en, es...</mat-hint>
          <mat-error>{{ erro('idioma') }}</mat-error>
        </mat-form-field>
      </div>

      <mat-form-field appearance="outline">
        <mat-label>Assuntos</mat-label>
        <textarea matInput formControlName="assuntos" rows="2"></textarea>
        <mat-hint>Um por linha.</mat-hint>
        <mat-error>{{ erro('assuntos') }}</mat-error>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Sinopse</mat-label>
        <textarea matInput formControlName="sinopse" rows="5"></textarea>
        <mat-error>{{ erro('sinopse') }}</mat-error>
      </mat-form-field>

      <div class="acoes">
        <button mat-flat-button type="submit" [disabled]="salvando()">
          @if (salvando()) {
            <mat-spinner diameter="18" />
          }
          {{ textoSalvar() }}
        </button>
        <button mat-button type="button" (click)="cancelar.emit()" [disabled]="salvando()">Cancelar</button>
      </div>
    </form>
  `,
  styles: `
    /* Ano, páginas e idioma lado a lado quando cabem; cada um desce sozinho quando a largura acaba. */
    .linha {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(140px, 100%), 1fr));
      gap: 20px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class FormularioObra {
  readonly inicial = input.required<DadosFormularioObra>();
  readonly isbnEditavel = input(false);
  readonly salvando = input(false);
  readonly textoSalvar = input('Salvar');
  readonly errosServidor = input<CampoInvalido[]>([]);

  readonly salvar = output<ObraRequisicao>();
  readonly cancelar = output<void>();
  /** Erros do servidor sem campo no formulário. */
  readonly errosSemCampo = output<string[]>();

  private readonly fb = inject(FormBuilder).nonNullable;
  private fonteDados: FonteIsbn | null = null;

  protected readonly form = this.fb.group({
    isbn: ['', [Validators.required]],
    titulo: ['', [Validators.required, Validators.maxLength(500)]],
    subtitulo: ['', [Validators.maxLength(500)]],
    autores: [''],
    editora: ['', [Validators.maxLength(255)]],
    ano: this.fb.control<number | null>(null, [Validators.min(1400), Validators.max(2100)]),
    paginas: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(99999)]),
    idioma: ['', [Validators.maxLength(10)]],
    assuntos: [''],
    sinopse: ['', [Validators.maxLength(20000)]],
  });

  constructor() {
    effect(() => {
      const d = this.inicial();
      this.fonteDados = d.fonteDados ?? null;
      this.form.reset({
        isbn: d.isbn,
        titulo: d.titulo ?? '',
        subtitulo: d.subtitulo ?? '',
        autores: (d.autores ?? []).join('\n'),
        editora: d.editora ?? '',
        ano: d.ano ?? null,
        paginas: d.paginas ?? null,
        idioma: d.idioma ?? '',
        assuntos: (d.assuntos ?? []).join('\n'),
        sinopse: d.sinopse ?? '',
      });
    });
    effect(() => {
      const campos = this.errosServidor();
      if (campos.length) {
        this.errosSemCampo.emit(aplicarErrosDoServidor(this.form, campos));
      }
    });
  }

  protected erro(campo: string): string {
    return mensagemDoCampo(this.form.get(campo));
  }

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvar.emit({
      isbn: v.isbn.trim(),
      titulo: v.titulo.trim(),
      subtitulo: textoOuNulo(v.subtitulo),
      autores: linhas(v.autores),
      editora: textoOuNulo(v.editora),
      ano: v.ano,
      paginas: v.paginas,
      idioma: textoOuNulo(v.idioma),
      sinopse: textoOuNulo(v.sinopse),
      assuntos: linhas(v.assuntos),
      fonteDados: this.fonteDados,
    });
  }
}
