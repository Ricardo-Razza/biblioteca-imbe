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
    <app-capa-livro [obraId]="e().exemplar.obraId" [temCapa]="e().exemplar.obraTemCapa" [mini]="true" [titulo]="e().exemplar.titulo" />
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
    <app-selo [tom]="selo().tom">{{ selo().texto }}</app-selo>
    <div class="acoes-item"><ng-content /></div>
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 10px 12px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      --largura-capa: 40px;
    }
    .info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
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
      gap: 4px;
    }
    @media (max-width: 640px) {
      :host {
        flex-wrap: wrap;
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
