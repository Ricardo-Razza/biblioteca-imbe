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
    <span class="nome" [style.font-size]="tamanhoNome()">Sala Literária<br />Castelinho</span>
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
      letter-spacing: 0.02em;
      text-transform: uppercase;
      /* Sem nowrap: sem espaço para as duas linhas (menu da gestão), quebra em três. */
    }
  `,
  host: { '[class.branca]': 'branca()' },
})
export class Assinatura {
  /** Altura da marca em px. 85 px dá os 100 px de largura mínimos do manual. */
  readonly altura = input(85);
  readonly branca = input(false);
  /** Tamanho do nome em px; sem valor, acompanha a altura da marca. */
  readonly tamanho = input<number>();
  // Em tela muito estreita o nome encolhe junto (a marca não passa dos 100 px mínimos do manual).
  protected readonly tamanhoNome = computed(
    () => `min(${this.tamanho() ?? Math.max(13, Math.round(this.altura() * 0.17))}px, 4.2vw)`,
  );
}
