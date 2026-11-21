import { AbstractControl, FormGroup } from '@angular/forms';

import { CampoInvalido } from '../core/api/erro-api';

/** Mensagem do primeiro erro do controle, em português. */
export function mensagemDoCampo(controle: AbstractControl | null): string {
  const erros = controle?.errors;
  if (!erros) {
    return '';
  }
  if (erros['servidor']) return erros['servidor'];
  if (erros['required']) return 'Campo obrigatório.';
  if (erros['maxlength']) return `Use no máximo ${erros['maxlength'].requiredLength} caracteres.`;
  if (erros['min']) return `O mínimo é ${erros['min'].min}.`;
  if (erros['max']) return `O máximo é ${erros['max'].max}.`;
  if (erros['pattern']) return 'Formato inválido.';
  if (erros['futura']) return 'A data não pode ser futura.';
  if (erros['data']) return 'Data inválida. Confira o dia, o mês e o ano.';
  if (erros['telefone']) return 'Telefone inválido. Use o DDD, ex.: (51) 99999-9999.';
  return 'Valor inválido.';
}

/**
 * Marca no formulário os erros que o backend devolveu (400 "Dados inválidos").
 * `autores[0]` vai para o campo `autores`. Devolve as mensagens sem campo correspondente.
 */
export function aplicarErrosDoServidor(form: FormGroup, campos: CampoInvalido[]): string[] {
  const sobras: string[] = [];
  for (const { campo, mensagem } of campos) {
    const nome = campo.replace(/\[\d+\]$/, '');
    const controle = form.get(nome);
    if (controle) {
      controle.setErrors({ ...(controle.errors ?? {}), servidor: mensagem });
      controle.markAsTouched();
    } else {
      sobras.push(mensagem);
    }
  }
  return sobras;
}

/** Uma entrada por linha, sem linhas vazias. */
export function linhas(texto: string | null | undefined): string[] {
  return (texto ?? '')
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0);
}

/** Texto em branco vira null. */
export function textoOuNulo(texto: string | null | undefined): string | null {
  const t = (texto ?? '').trim();
  return t ? t : null;
}

/** Hoje em yyyy-MM-dd no fuso do navegador (para input type="date"). */
export function hojeIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}
