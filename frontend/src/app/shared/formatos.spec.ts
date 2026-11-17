import { cpfValido, dataBr, dias, formatarCpf } from './formatos';
import { seloEmprestimo } from '../gestao/emprestimos/item-emprestimo';
import { Emprestimo } from '../core/api/modelos';

describe('formatos', () => {
  it('formata CPF com pontos e traço', () => {
    expect(formatarCpf('12345678909')).toBe('123.456.789-09');
    expect(formatarCpf('123')).toBe('123');
    expect(formatarCpf(null)).toBe('');
  });

  it('mostra data ISO em dd/mm/aaaa sem mexer no fuso', () => {
    expect(dataBr('2026-01-01')).toBe('01/01/2026');
    expect(dataBr('2026-11-17T23:30:00')).toBe('17/11/2026');
    expect(dataBr(null)).toBe('');
  });

  it('escreve dias no singular e plural', () => {
    expect(dias(1)).toBe('1 dia');
    expect(dias(3)).toBe('3 dias');
  });

  it('confere os dígitos do CPF', () => {
    expect(cpfValido('123.456.789-09')).toBe(true);
    expect(cpfValido('12345678900')).toBe(false);
    expect(cpfValido('11111111111')).toBe(false);
  });
});

describe('seloEmprestimo', () => {
  const base = {
    status: 'ATIVO',
    dataPrevista: '2026-11-20',
    diasAtraso: 0,
    dataDevolucao: null,
    ocorrenciaDevolucao: null,
  } as unknown as Emprestimo;

  it('avisa quando vence hoje', () => {
    expect(seloEmprestimo({ ...base, dataPrevista: '2026-11-17' }, '2026-11-17')).toEqual({ texto: 'Vence hoje', tom: 'alerta' });
  });

  it('mostra a data de devolução quando ainda está no prazo', () => {
    expect(seloEmprestimo(base, '2026-11-17')).toEqual({ texto: 'Vence em 20/11/2026', tom: 'ok' });
  });

  it('mostra o atraso', () => {
    expect(seloEmprestimo({ ...base, status: 'ATRASADO', diasAtraso: 2 }, '2026-11-17').texto).toBe('Atrasado 2 dias');
  });

  it('mostra a devolução com a ocorrência', () => {
    const e = { ...base, status: 'DEVOLVIDO', dataDevolucao: '2026-11-10', ocorrenciaDevolucao: 'DANIFICADO' } as Emprestimo;
    expect(seloEmprestimo(e).texto).toBe('Devolvido em 10/11/2026 (danificado)');
  });
});
