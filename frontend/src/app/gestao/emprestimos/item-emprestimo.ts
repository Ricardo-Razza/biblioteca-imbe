import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Emprestimo } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { dataBr, dias } from '../../shared/formatos';
import { Selo, TomSelo } from '../../shared/selo/selo';

/** Situação do empréstimo em texto: "Atrasado 3 dias", "Vence hoje", "Devolvido em 10/11/2026". */
export function seloEmprestimo(e: Emprestimo, hoje = hojeIso()): { texto: string; tom: TomSelo } {
  if (e.status === 'DEVOLVIDO') {
    const ocorrencia = e.ocorrenciaDevolucao === 'EXTRAVIADO' ? ' (extraviado)' : e.ocorrenciaDevolucao === 'DANIFICADO' ? ' (danificado)' : '';
    return { texto: `Devolvido em ${dataBr(e.dataDevolucao)}${ocorrencia}`, tom: 'neutra' };
  }
  if (e.status === 'ATRASADO') {
    return { texto: `Atrasado ${dias(e.diasAtraso)}`, tom: 'erro' };
  }
  if (e.dataPrevista === hoje) {
    return { texto: 'Vence hoje', tom: 'alerta' };
  }
  return { texto: `Vence em ${dataBr(e.dataPrevista)}`, tom: 'ok' };
}

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Linha de empréstimo: capa, título, tombo, leitor e situação. Ações entram pelo conteúdo. */
@Component({
  selector: 'app-item-emprestimo',
  imports: [RouterLink, CapaLivro, Selo],
  template: `
    <div class="linha">
    <app-capa-livro class="capa" [obraId]="e().exemplar.obraId" [temCapa]="e().exemplar.obraTemCapa" [mini]="true" [titulo]="e().exemplar.titulo" />
    <div class="info">
      <a class="titulo" [routerLink]="['/painel/acervo', e().exemplar.obraId]">{{ e().exemplar.titulo }}</a>
      <span class="texto-secundario">Tombo {{ e().exemplar.tombo }} · saiu em {{ data(e().dataSaida) }}
        @if (e().renovacoes) {
          · renovado {{ e().renovacoes === 1 ? '1 vez' : e().renovacoes + ' vezes' }}
        }
      </span>
      @if (mostrarLeitor()) {
        <a class="leitor" [routerLink]="['/painel/leitores', e().leitor.id]">{{ e().leitor.nome }}</a>
      }
      @if (e().observacao) {
        <span class="texto-secundario">{{ e().observacao }}</span>
      }
    </div>
    <app-selo class="selo" [tom]="selo().tom">{{ selo().texto }}</app-selo>
    <div class="acoes-item"><ng-content /></div>
    </div>
  `,
  styles: `
    /* Container query: a largura que conta é a do espaço da lista, não a da janela (o menu da gestão ocupa parte). */
    :host {
      display: block;
      container-type: inline-size;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      --largura-capa: 40px;
    }
    .linha {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr) auto auto;
      align-items: center;
      gap: 8px 16px;
      padding: 10px 12px;
    }
    .info {
      min-width: 0;
      display: flex;
      flex-direction: column;
      overflow-wrap: anywhere;
    }
    .titulo {
      font-weight: 500;
      color: inherit;
      text-decoration: none;
    }
    .titulo:hover,
    .leitor:hover {
      text-decoration: underline;
    }
    .leitor {
      color: var(--mat-sys-primary);
      text-decoration: none;
    }
    .acoes-item {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
    /* Estreito: capa e dados em cima; situação e botões numa linha embaixo, alinhados com o texto */
    @container (max-width: 640px) {
      .linha {
        grid-template-columns: auto minmax(0, 1fr);
        align-items: start;
        padding: 12px 4px;
        gap: 8px 12px;
      }
      .selo,
      .acoes-item {
        grid-column: 2;
        justify-self: start;
      }
    }
  `,
})
export class ItemEmprestimo {
  readonly e = input.required<Emprestimo>();
  readonly mostrarLeitor = input(true);
  protected readonly selo = computed(() => seloEmprestimo(this.e()));
  protected readonly data = dataBr;
}
