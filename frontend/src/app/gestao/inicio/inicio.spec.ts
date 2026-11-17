import { diasAntes, nomeDoArquivo } from '../relatorios/relatorios';
import { diaDaSemana, tetoDoEixo } from './movimento-semana';

describe('painel e relatórios', () => {
  it('dia da semana sem fuso', () => {
    expect(diaDaSemana('2026-11-17')).toBe('ter');
    expect(diaDaSemana('2026-11-22')).toBe('dom');
  });

  it('teto do eixo', () => {
    expect(tetoDoEixo(0)).toBe(4);
    expect(tetoDoEixo(3)).toBe(4);
    expect(tetoDoEixo(5)).toBe(8);
    expect(tetoDoEixo(12)).toBe(12);
  });

  it('período padrão', () => {
    expect(diasAntes('2026-11-17', 29)).toBe('2026-10-19');
    expect(diasAntes('2026-03-01', 1)).toBe('2026-02-28');
  });

  it('nome do arquivo do Content-Disposition', () => {
    expect(nomeDoArquivo('attachment; filename="acervo-2026-11-17.csv"', 'x.csv')).toBe('acervo-2026-11-17.csv');
    expect(nomeDoArquivo(null, 'x.csv')).toBe('x.csv');
  });
});
