import { diasAntes, nomeDoArquivo } from '../relatorios/relatorios';
import { Painel } from '../../core/api/modelos';
import { detalhes } from './inicio';
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

  it('linha de contexto dos indicadores', () => {
    const base = {
      hoje: '2026-11-19',
      contadores: { emprestados: 5, atrasados: 2, venceHoje: 0, reservasParaSeparar: 2, leitoresBloqueados: 0 },
      venceHoje: [],
      atrasados: [{ diasAtraso: 3 }, { diasAtraso: 12 }],
      paraSeparar: [{ prazoRetirada: '2026-11-19' }, { prazoRetirada: '2026-11-23' }],
      movimento: [{ data: '2026-11-19', saidas: 4, devolucoes: 1 }],
    } as unknown as Painel;
    expect(detalhes(base)).toEqual({
      emprestados: '+4 hoje',
      atrasados: 'mais antigo há 12 dias',
      separados: '1 com prazo hoje',
    });
    expect(detalhes({ ...base, atrasados: [], paraSeparar: [], movimento: [] })).toEqual({
      emprestados: 'nenhuma saída hoje',
      atrasados: 'nenhum em atraso',
      separados: 'nenhum com prazo hoje',
    });
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
