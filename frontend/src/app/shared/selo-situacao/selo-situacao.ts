import { Component, computed, input } from '@angular/core';

import { SituacaoExemplar } from '../../core/api/modelos';

const SELOS: Record<SituacaoExemplar, { texto: string; tom: 'ok' | 'alerta' | 'erro' | 'neutra' }> = {
  DISPONIVEL: { texto: 'Disponível', tom: 'ok' },
  RESERVADO: { texto: 'Reservado', tom: 'alerta' },
  EMPRESTADO: { texto: 'Emprestado', tom: 'alerta' },
  EXTRAVIADO: { texto: 'Extraviado', tom: 'erro' },
  BAIXADO: { texto: 'Baixado', tom: 'neutra' },
};

/** Situação do exemplar: cor e sempre o texto junto. */
@Component({
  selector: 'app-selo-situacao',
  template: `{{ selo().texto }}`,
  host: { '[class]': '"selo " + selo().tom' },
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
export class SeloSituacao {
  readonly situacao = input.required<SituacaoExemplar>();
  protected readonly selo = computed(() => SELOS[this.situacao()]);
}
