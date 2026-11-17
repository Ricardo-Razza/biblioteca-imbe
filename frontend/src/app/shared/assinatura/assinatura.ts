import { Component, computed, input } from '@angular/core';

/**
 * Assinatura da biblioteca no padrão das secretarias do manual de marca da Prefeitura (pág. 18):
 * marca vertical 1 sem o conceito, fio vertical e o nome alinhado à esquerda.
 * `branca` usa a versão monocromática negativa, para fundo escuro.
 */
@Component({
  selector: 'app-assinatura',
  template: `
    <img
      class="marca"
      [src]="branca() ? 'marca/imbe-vertical-branca.png' : 'marca/imbe-vertical.png'"
      alt="Prefeitura de Imbé"
      [style.height.px]="altura()"
    />
    <span class="fio" aria-hidden="true"></span>
    <span class="nome" [style.font-size.px]="tamanhoNome()">Biblioteca<br />Municipal</span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      color: var(--marca-azul);
    }
    :host(.branca) {
      color: #fff;
    }
    .marca {
      display: block;
      width: auto;
      /* Manual, pág. 15: em meio eletrônico a vertical 1 não fica com menos de 100 px de largura. */
      min-width: 100px;
      object-fit: contain;
    }
    .fio {
      align-self: stretch;
      width: 1.5px;
      background: currentColor;
      margin: 4px 0;
    }
    .nome {
      font-family: var(--fonte-marca);
      font-weight: 500;
      line-height: 1.25;
      letter-spacing: 0.01em;
    }
  `,
  host: { '[class.branca]': 'branca()' },
})
export class Assinatura {
  /** Altura da marca em px. 85 px dá os 100 px de largura mínimos do manual. */
  readonly altura = input(85);
  readonly branca = input(false);
  protected readonly tamanhoNome = computed(() => Math.max(13, Math.round(this.altura() * 0.17)));
}
