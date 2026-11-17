import { HttpErrorResponse } from '@angular/common/http';

import { camposComErro, mensagemDeErro } from './erro-api';

describe('mensagemDeErro', () => {
  it('usa o detail do ProblemDetail', () => {
    const erro = new HttpErrorResponse({ status: 409, error: { title: 'Conflito', detail: 'Já existe.' } });
    expect(mensagemDeErro(erro)).toBe('Já existe.');
  });

  it('explica a falta de conexão', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toContain('Sem conexão');
  });

  it('tem texto genérico para 500 sem corpo', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 502, error: null }))).toContain('servidor');
  });
});

describe('camposComErro', () => {
  it('devolve os campos do 400', () => {
    const campos = [{ campo: 'tombo', mensagem: 'Informe o tombo.' }];
    const erro = new HttpErrorResponse({ status: 400, error: { detail: 'Há campos inválidos.', campos } });
    expect(camposComErro(erro)).toEqual(campos);
  });

  it('é vazio para outros erros', () => {
    expect(camposComErro(new HttpErrorResponse({ status: 422, error: { detail: 'x' } }))).toEqual([]);
  });
});
