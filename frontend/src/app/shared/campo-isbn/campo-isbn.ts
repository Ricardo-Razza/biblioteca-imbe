import { Component, ElementRef, afterNextRender, effect, input, output, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { validarIsbn } from '../isbn';

/**
 * Campo para bipar ou digitar o ISBN. O leitor USB "digita" o código e manda Enter, que dispara a busca.
 * Valida o dígito verificador antes de emitir; emite sempre o ISBN-13.
 */
@Component({
  selector: 'app-campo-isbn',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <form class="campo" (submit)="enviar($event)">
      <mat-form-field appearance="outline">
        <mat-label>ISBN</mat-label>
        <mat-icon matPrefix>barcode_scanner</mat-icon>
        <input
          #entrada
          matInput
          [formControl]="controle"
          inputmode="numeric"
          autocomplete="off"
          maxlength="20"
        />
        <mat-hint>Bipe o código de barras da contracapa ou digite o número.</mat-hint>
        <mat-error>{{ erro() }}</mat-error>
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="carregando()">
        @if (carregando()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>search</mat-icon>
        }
        Buscar
      </button>
    </form>
  `,
  styles: `
    .campo {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      max-width: 640px;
    }
    mat-form-field {
      flex: 1;
    }
    button {
      margin-top: 4px;
      height: 48px;
    }
    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `,
})
export class CampoIsbn {
  readonly carregando = input(false);
  readonly buscar = output<string>();

  protected readonly controle = new FormControl('', { nonNullable: true });
  protected readonly erro = signal('');
  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');

  constructor() {
    afterNextRender(() => this.focar());
    effect(() => {
      if (this.carregando()) {
        this.controle.disable();
      } else {
        this.controle.enable();
      }
    });
  }

  focar(): void {
    const el = this.entrada().nativeElement;
    el.focus();
    el.select();
  }

  limpar(): void {
    this.controle.reset('');
    this.erro.set('');
    this.focar();
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    if (this.carregando()) {
      return;
    }
    const resultado = validarIsbn(this.controle.value);
    if ('erro' in resultado) {
      this.erro.set(resultado.erro);
      this.controle.setErrors({ isbn: resultado.erro });
      this.controle.markAsTouched();
      this.focar();
      return;
    }
    this.erro.set('');
    this.buscar.emit(resultado.isbn13);
  }
}
