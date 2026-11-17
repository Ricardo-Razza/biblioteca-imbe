import { Component, computed, input, signal } from '@angular/core';

import { DiaMovimento } from '../../core/api/modelos';
import { dataBr } from '../../shared/formatos';

const DIAS_SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

/** "ter" para 2026-11-17, sem passar por fuso. */
export function diaDaSemana(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number);
  return DIAS_SEMANA[new Date(a, m - 1, d).getDay()];
}

/** Teto "redondo" do eixo: 4, 8, 12... com no mínimo 4 para a semana vazia não virar um gráfico achatado. */
export function tetoDoEixo(maximo: number): number {
  return Math.max(4, Math.ceil(maximo / 4) * 4);
}

/**
 * Barras agrupadas de saídas e devoluções nos últimos sete dias. Uma escala só, legenda sempre visível,
 * tooltip ao passar o mouse ou focar o dia, e a tabela com os números para leitor de tela.
 */
@Component({
  selector: 'app-movimento-semana',
  template: `
    <div class="legenda" aria-hidden="true">
      <span><i class="marca saidas"></i>Saídas ({{ totais().saidas }})</span>
      <span><i class="marca devolucoes"></i>Devoluções ({{ totais().devolucoes }})</span>
    </div>

    <div class="grafico" aria-hidden="true">
      <div class="eixo">
        <span>{{ teto() }}</span>
        <span>{{ teto() / 2 }}</span>
        <span>0</span>
      </div>
      <div class="area">
        <div class="grade"><i></i><i></i><i></i></div>
        @for (d of dias(); track d.data; let i = $index) {
          <div
            class="dia"
            tabindex="0"
            (mouseenter)="ativo.set(i)"
            (mouseleave)="ativo.set(null)"
            (focus)="ativo.set(i)"
            (blur)="ativo.set(null)"
          >
            <div class="barras">
              <i class="barra saidas" [style.height.%]="(d.saidas / teto()) * 100"></i>
              <i class="barra devolucoes" [style.height.%]="(d.devolucoes / teto()) * 100"></i>
            </div>
            <span class="rotulo" [class.hoje]="$last">{{ $last ? 'hoje' : semana(d.data) }}</span>
            @if (ativo() === i) {
              <div class="dica" [class.direita]="i >= 4">
                <strong>{{ semana(d.data) }}, {{ data(d.data) }}</strong>
                <span><i class="marca saidas"></i>{{ d.saidas }} {{ d.saidas === 1 ? 'saída' : 'saídas' }}</span>
                <span><i class="marca devolucoes"></i>{{ d.devolucoes }} {{ d.devolucoes === 1 ? 'devolução' : 'devoluções' }}</span>
              </div>
            }
          </div>
        }
      </div>
    </div>

    <table class="so-leitor-de-tela">
      <caption>Saídas e devoluções nos últimos sete dias</caption>
      <thead>
        <tr><th scope="col">Dia</th><th scope="col">Saídas</th><th scope="col">Devoluções</th></tr>
      </thead>
      <tbody>
        @for (d of dias(); track d.data) {
          <tr><th scope="row">{{ data(d.data) }}</th><td>{{ d.saidas }}</td><td>{{ d.devolucoes }}</td></tr>
        }
      </tbody>
    </table>
  `,
  styles: `
    :host {
      display: block;
      --serie-saidas: var(--marca-azul);
      --serie-devolucoes: var(--marca-coral);
    }
    .legenda {
      display: flex;
      gap: 20px;
      margin-bottom: 12px;
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .marca {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 2px;
      margin-right: 6px;
      vertical-align: -1px;
    }
    .saidas {
      background: var(--serie-saidas);
    }
    .devolucoes {
      background: var(--serie-devolucoes);
    }
    .grafico {
      display: flex;
      gap: 8px;
      height: 180px;
    }
    .eixo {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding-bottom: 24px;
      font: var(--mat-sys-label-small);
      color: var(--mat-sys-on-surface-variant);
      text-align: right;
      min-width: 16px;
      line-height: 0;
    }
    .area {
      position: relative;
      flex: 1;
      display: flex;
      gap: 4px;
    }
    .grade {
      position: absolute;
      inset: 0 0 24px 0;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      pointer-events: none;
    }
    .grade i {
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
    .grade i:last-child {
      border-top-color: var(--mat-sys-outline);
    }
    .dia {
      position: relative;
      flex: 1;
      display: flex;
      flex-direction: column;
      border-radius: 6px;
      outline: none;
    }
    .dia:hover .barras,
    .dia:focus-visible .barras {
      background: color-mix(in srgb, var(--mat-sys-on-surface) 5%, transparent);
    }
    .dia:focus-visible {
      box-shadow: 0 0 0 2px var(--mat-sys-primary);
    }
    .barras {
      flex: 1;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      gap: 2px;
      border-radius: 6px 6px 0 0;
    }
    .barra {
      width: min(18px, 30%);
      border-radius: 4px 4px 0 0;
      min-height: 0;
    }
    .rotulo {
      height: 24px;
      line-height: 24px;
      text-align: center;
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .rotulo.hoje {
      color: var(--mat-sys-on-surface);
      font-weight: 600;
    }
    .dica {
      position: absolute;
      bottom: calc(100% - 20px);
      left: 50%;
      z-index: 2;
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 8px 12px;
      border-radius: 8px;
      background: var(--mat-sys-inverse-surface);
      color: var(--mat-sys-inverse-on-surface);
      font: var(--mat-sys-body-small);
      white-space: nowrap;
      pointer-events: none;
    }
    .dica.direita {
      left: auto;
      right: 50%;
    }
    .so-leitor-de-tela {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip-path: inset(50%);
      white-space: nowrap;
    }
  `,
})
export class MovimentoSemana {
  readonly dias = input.required<DiaMovimento[]>();

  protected readonly ativo = signal<number | null>(null);
  protected readonly teto = computed(() => tetoDoEixo(Math.max(0, ...this.dias().flatMap((d) => [d.saidas, d.devolucoes]))));
  protected readonly totais = computed(() =>
    this.dias().reduce((t, d) => ({ saidas: t.saidas + d.saidas, devolucoes: t.devolucoes + d.devolucoes }), { saidas: 0, devolucoes: 0 }),
  );
  protected readonly data = dataBr;
  protected readonly semana = diaDaSemana;
}
