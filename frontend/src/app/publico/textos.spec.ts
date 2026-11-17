import { Reserva, ReservaPublica } from '../core/api/modelos';
import { seloReserva } from '../gestao/reservas/reservas-api';
import { dataComDia, disponibilidade, protocoloLegivel, seloReservaPublica } from './textos';

describe('textos do site público', () => {
  it('disponibilidade', () => {
    expect(disponibilidade(0)).toBe('Todos emprestados');
    expect(disponibilidade(1)).toBe('1 disponível');
    expect(disponibilidade(3)).toBe('3 disponíveis');
  });

  it('protocolo e data com dia da semana', () => {
    expect(protocoloLegivel('ABCD2345')).toBe('ABCD-2345');
    expect(dataComDia('2026-11-19')).toBe('quinta-feira, 19/11/2026');
  });

  it('selo da reserva para o leitor', () => {
    const base = { prazoRetirada: '2026-11-19', finalizadaEm: '2026-11-18T10:00:00' } as ReservaPublica;
    expect(seloReservaPublica({ ...base, status: 'AGUARDANDO_RETIRADA' }).texto).toBe('Retire até 19/11/2026');
    expect(seloReservaPublica({ ...base, status: 'RETIRADA' }).texto).toBe('Retirada em 18/11/2026');
    expect(seloReservaPublica({ ...base, status: 'EXPIRADA' }).tom).toBe('alerta');
  });

  it('selo da reserva na gestão', () => {
    const base = { status: 'AGUARDANDO_RETIRADA', prazoRetirada: '2026-11-19', prazoVencido: false } as Reserva;
    expect(seloReserva(base, '2026-11-17')).toEqual({ texto: 'Retirar até 19/11/2026', tom: 'ok' });
    expect(seloReserva(base, '2026-11-19')).toEqual({ texto: 'Último dia hoje', tom: 'alerta' });
    expect(seloReserva({ ...base, prazoVencido: true }, '2026-11-20').tom).toBe('erro');
    expect(
      seloReserva({ ...base, status: 'CANCELADA', canceladaPor: 'LEITOR', finalizadaEm: '2026-11-18T09:00:00' }, '2026-11-20').texto,
    ).toBe('Cancelada pelo leitor em 18/11/2026');
  });
});
