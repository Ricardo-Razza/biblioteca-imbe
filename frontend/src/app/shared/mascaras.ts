import { Directive, ElementRef, inject, input } from '@angular/core';
import { AbstractControl, NgControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Dígitos do telefone com DDD (10 ou 11), sem o 55 do país. Nulo se não for um telefone válido. */
export function digitosTelefone(valor: string): string | null {
  let d = (valor ?? '').replace(/\D/g, '');
  if ((d.length === 12 || d.length === 13) && d.startsWith('55')) {
    d = d.slice(2);
  }
  return d.length === 10 || d.length === 11 ? d : null;
}

/** (51) 99999-9999 ou (51) 3333-4444. */
export function formatarTelefone(valor: string): string {
  const d = digitosTelefone(valor);
  if (!d) {
    return valor;
  }
  const meio = d.length - 4;
  return `(${d.slice(0, 2)}) ${d.slice(2, meio)}-${d.slice(meio)}`;
}

/** CPF enquanto digita: só números, no máximo 11, com pontos e traço. */
export function mascaraCpf(valor: string): string {
  const d = (valor ?? '').replace(/\D/g, '').slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
}

/** Telefone enquanto digita: DDD entre parênteses, no máximo 11 números (o 55 do país sai). */
export function mascaraTelefone(valor: string): string {
  let d = (valor ?? '').replace(/\D/g, '');
  if (d.length > 11 && d.startsWith('55')) {
    d = d.slice(2);
  }
  d = d.slice(0, 11);
  if (d.length <= 2) {
    return d.length ? `(${d}` : '';
  }
  const resto = d.slice(2);
  const corte = d.length === 11 ? 5 : 4;
  return `(${d.slice(0, 2)}) ${resto.length > corte ? `${resto.slice(0, corte)}-${resto.slice(corte)}` : resto}`;
}

/** Busca por nome ou CPF: começando por número, vira CPF (com máscara e limite); senão, texto livre. */
export function mascaraNomeOuCpf(valor: string): string {
  const v = valor ?? '';
  return /^[\d.\-\s]+$/.test(v) && /\d/.test(v) ? mascaraCpf(v) : v.slice(0, 150);
}

/** Tombo: letras, números, ponto e hífen, até 30, em maiúsculas (como o backend grava). */
export function mascaraTombo(valor: string): string {
  return (valor ?? '')
    .replace(/[^A-Za-z0-9.-]/g, '')
    .slice(0, 30)
    .toUpperCase();
}

export type TipoMascara = 'cpf' | 'telefone' | 'nome-ou-cpf' | 'tombo';

const MASCARAS: Record<TipoMascara, { aplicar: (v: string) => string; maximo: number; modo: string }> = {
  cpf: { aplicar: mascaraCpf, maximo: 14, modo: 'numeric' },
  telefone: { aplicar: mascaraTelefone, maximo: 19, modo: 'tel' },
  'nome-ou-cpf': { aplicar: mascaraNomeOuCpf, maximo: 150, modo: 'text' },
  tombo: { aplicar: mascaraTombo, maximo: 30, modo: 'text' },
};

/**
 * Máscara enquanto digita: `<input appMascara="cpf">`. Limita o tamanho, ajusta o teclado do celular e, num
 * campo de formulário, grava no controle o valor já formatado.
 */
@Directive({
  selector: 'input[appMascara]',
  host: {
    '(input)': 'aplicar()',
    '[attr.maxlength]': 'config().maximo',
    '[attr.inputmode]': 'config().modo',
  },
})
export class Mascara {
  readonly tipo = input.required<TipoMascara>({ alias: 'appMascara' });
  private readonly el = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly controle = inject(NgControl, { optional: true, self: true });

  protected config() {
    return MASCARAS[this.tipo()];
  }

  protected aplicar(): void {
    const el = this.el.nativeElement;
    const novo = this.config().aplicar(el.value);
    if (novo !== el.value) {
      el.value = novo;
    }
    if (this.controle?.control && this.controle.control.value !== novo) {
      this.controle.control.setValue(novo);
    }
  }
}

/** "aaaa-mm-dd" de hoje, no fuso do navegador. */
function hoje(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * Data entre `minimo` e `maximo` (padrão: hoje). O campo de data do navegador aceita qualquer ano, até 0221:
 * fora da faixa é erro de digitação. Erro `data`.
 */
export function dataEntre(minimo: string, maximo?: string): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = c.value as string | null;
    return !v || (v >= minimo && v <= (maximo ?? hoje())) ? null : { data: true };
  };
}

/** Mais antiga data de nascimento aceita. */
export const NASCIMENTO_MINIMO = '1900-01-01';
