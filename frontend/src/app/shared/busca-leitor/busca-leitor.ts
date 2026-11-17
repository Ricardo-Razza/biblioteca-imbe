import { Component, DestroyRef, ElementRef, afterNextRender, inject, input, output, signal, viewChild } from '@angular/core';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { Leitor } from '../../core/api/modelos';
import { LeitoresApi } from '../../gestao/leitores/leitores-api';
import { formatarCpf } from '../formatos';

/** Procura o leitor pelo nome ou pelo CPF e emite o escolhido. */
@Component({
  selector: 'app-busca-leitor',
  imports: [MatAutocompleteModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <mat-form-field appearance="outline" class="campo" subscriptSizing="dynamic">
      <mat-label>{{ rotulo() }}</mat-label>
      <mat-icon matPrefix>person_search</mat-icon>
      <input
        #entrada
        matInput
        [matAutocomplete]="auto"
        autocomplete="off"
        (input)="digitar($any($event.target).value)"
      />
      <mat-hint>Nome ou CPF.</mat-hint>
      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="escolher($event)" [displayWith]="exibir">
        @for (l of opcoes(); track l.id) {
          <mat-option [value]="l">
            <span class="nome">{{ l.nome }}</span>
            <span class="texto-secundario"> · CPF {{ cpf(l.cpf) }}{{ l.ativo ? '' : ' · desativado' }}</span>
          </mat-option>
        }
        @if (buscou() && !opcoes().length) {
          <mat-option disabled>Nenhum leitor encontrado.</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: `
    .campo {
      width: 100%;
      max-width: 640px;
    }
    .nome {
      font-weight: 500;
    }
  `,
})
export class BuscaLeitor {
  readonly rotulo = input('Leitor');
  readonly focarAoAbrir = input(false);
  readonly selecionado = output<Leitor>();

  private readonly api = inject(LeitoresApi);
  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
  protected readonly opcoes = signal<Leitor[]>([]);
  protected readonly buscou = signal(false);
  private espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    afterNextRender(() => {
      if (this.focarAoAbrir()) this.entrada().nativeElement.focus();
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
  }

  protected cpf = formatarCpf;
  protected exibir = (l: Leitor | string | null) => (typeof l === 'string' ? l : (l?.nome ?? ''));

  limpar(): void {
    this.entrada().nativeElement.value = '';
    this.opcoes.set([]);
    this.buscou.set(false);
  }

  protected digitar(texto: string): void {
    clearTimeout(this.espera);
    if (texto.trim().length < 2) {
      this.opcoes.set([]);
      this.buscou.set(false);
      return;
    }
    this.espera = setTimeout(() => {
      this.api.listar(texto, 0, 10).subscribe({
        next: (p) => {
          this.opcoes.set(p.itens);
          this.buscou.set(true);
        },
        error: () => this.opcoes.set([]),
      });
    }, 250);
  }

  protected escolher(e: MatAutocompleteSelectedEvent): void {
    this.selecionado.emit(e.option.value as Leitor);
  }
}
