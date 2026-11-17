/**
 * Validação de ISBN no navegador, igual à do backend (Isbn.de): aceita ISBN-10 ou ISBN-13, com ou sem hífens
 * e espaços, confere o dígito verificador e devolve o ISBN-13. Serve para avisar antes de chamar a API.
 */
export type ResultadoIsbn = { isbn13: string } | { erro: string };

export function validarIsbn(entrada: string): ResultadoIsbn {
  const limpo = entrada.replace(/[\s-]/g, '').toUpperCase();
  if (!limpo) {
    return { erro: 'Informe o ISBN.' };
  }
  if (/^\d{13}$/.test(limpo)) {
    if (!limpo.startsWith('978') && !limpo.startsWith('979')) {
      return { erro: 'Este código de barras não é de livro. O ISBN começa com 978 ou 979.' };
    }
    if (digito13(limpo.slice(0, 12)) !== limpo[12]) {
      return { erro: 'ISBN inválido: o dígito verificador não confere. Confira o número.' };
    }
    return { isbn13: limpo };
  }
  if (/^\d{9}[\dX]$/.test(limpo)) {
    if (digito10(limpo.slice(0, 9)) !== limpo[9]) {
      return { erro: 'ISBN inválido: o dígito verificador não confere. Confira o número.' };
    }
    const base = '978' + limpo.slice(0, 9);
    return { isbn13: base + digito13(base) };
  }
  return { erro: 'O ISBN tem 10 ou 13 dígitos.' };
}

function digito13(doze: string): string {
  let soma = 0;
  for (let i = 0; i < 12; i++) {
    soma += Number(doze[i]) * (i % 2 === 0 ? 1 : 3);
  }
  return String((10 - (soma % 10)) % 10);
}

function digito10(nove: string): string {
  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += Number(nove[i]) * (10 - i);
  }
  const d = (11 - (soma % 11)) % 11;
  return d === 10 ? 'X' : String(d);
}
