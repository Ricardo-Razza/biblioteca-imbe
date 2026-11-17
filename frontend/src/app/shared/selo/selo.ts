import { Component, input } from '@angular/core';

export type TomSelo = 'ok' | 'alerta' | 'erro' | 'neutra';

/** Etiqueta de situação: cor e sempre um texto ("Atrasado 3 dias"). */
@Component({
  selector: 'app-selo',
  template: `<ng-content />`,
  host: { '[class]': '"selo " + tom()' },
  styles: `
    :host {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 999px;
      font: var(--mat-sys-label-large);
      white-space: nowrap;
    }
    :host(.ok) {
      background: var(--situacao-ok-fundo);
      color: var(--situacao-ok);
    }
    :host(.alerta) {
      background: var(--situacao-alerta-fundo);
      color: var(--situacao-alerta);
    }
    :host(.erro) {
      background: var(--situacao-erro-fundo);
      color: var(--situacao-erro);
    }
    :host(.neutra) {
      background: var(--situacao-neutra-fundo);
      color: var(--situacao-neutra);
    }
  `,
})
export class Selo {
  readonly tom = input<TomSelo>('neutra');
}
