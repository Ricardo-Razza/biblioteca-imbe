import { ReservaPublica } from '../core/api/modelos';
import { dataBr } from '../shared/formatos';
import { TomSelo } from '../shared/selo/selo';

const DIAS_SEMANA = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];

/** "3 na estante", "1 na estante", "Todos emprestados". */
export function disponibilidade(n: number): string {
  if (n <= 0) return 'Todos emprestados';
  return `${n} na estante`;
}

/** ABCD2345 → ABCD-2345, mais fácil de ler e anotar. */
export function protocoloLegivel(p: string): string {
  return p.length === 8 ? `${p.slice(0, 4)}-${p.slice(4)}` : p;
}

/** 2026-11-19 → "quarta-feira, 19/11/2026" (sem passar por fuso). */
export function dataComDia(iso: string): string {
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  return `${DIAS_SEMANA[new Date(a, m - 1, d).getDay()]}, ${dataBr(iso)}`;
}

/** Situação da reserva para o próprio leitor. */
export function seloReservaPublica(r: ReservaPublica): { texto: string; tom: TomSelo } {
  switch (r.status) {
    case 'AGUARDANDO_RETIRADA':
      return { texto: `Retire até ${dataBr(r.prazoRetirada)}`, tom: 'ok' };
    case 'RETIRADA':
      return { texto: `Retirada em ${dataBr(r.finalizadaEm)}`, tom: 'neutra' };
    case 'CANCELADA':
      return { texto: `Cancelada em ${dataBr(r.finalizadaEm)}`, tom: 'neutra' };
    case 'EXPIRADA':
      return { texto: 'Prazo de retirada encerrado', tom: 'alerta' };
  }
}
