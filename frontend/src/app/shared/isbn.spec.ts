import { validarIsbn } from './isbn';

describe('validarIsbn', () => {
  it('aceita ISBN-13 com hífens e espaços', () => {
    expect(validarIsbn(' 978-85-359-0277-8 ')).toEqual({ isbn13: '9788535902778' });
  });

  it('converte ISBN-10 para ISBN-13, inclusive com X', () => {
    expect(validarIsbn('85-359-0277-5')).toEqual({ isbn13: '9788535902778' });
    expect(validarIsbn('080442957x')).toEqual({ isbn13: '9780804429573' });
  });

  it('recusa dígito verificador errado', () => {
    expect(validarIsbn('9788535902779')).toEqual({ erro: expect.stringContaining('dígito verificador') });
    expect(validarIsbn('8535902776')).toEqual({ erro: expect.stringContaining('dígito verificador') });
  });

  it('recusa código de barras que não é de livro', () => {
    expect(validarIsbn('7891234567895')).toEqual({ erro: expect.stringContaining('978 ou 979') });
  });

  it('recusa vazio e tamanho errado', () => {
    expect(validarIsbn('  ')).toEqual({ erro: 'Informe o ISBN.' });
    expect(validarIsbn('12345')).toEqual({ erro: 'O ISBN tem 10 ou 13 dígitos.' });
  });
});
