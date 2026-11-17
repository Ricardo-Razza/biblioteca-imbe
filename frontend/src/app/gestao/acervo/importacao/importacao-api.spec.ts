import { progresso, textoResultado } from './importacao-api';

describe('importação', () => {
  it('progresso', () => {
    expect(progresso({ total: 0, pendentes: 0 })).toBe(100);
    expect(progresso({ total: 4, pendentes: 3 })).toBe(25);
    expect(progresso({ total: 3, pendentes: 0 })).toBe(100);
  });

  it('texto e tom do resultado', () => {
    expect(textoResultado('CADASTRADO')).toEqual({ texto: 'Cadastrado', tom: 'ok' });
    expect(textoResultado('FONTE_INDISPONIVEL').tom).toBe('alerta');
    expect(textoResultado('ERRO').tom).toBe('erro');
  });
});
