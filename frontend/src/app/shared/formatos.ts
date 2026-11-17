/** 12345678909 → 123.456.789-09 */
export function formatarCpf(cpf: string | null | undefined): string {
  const d = (cpf ?? '').replace(/\D/g, '');
  if (d.length !== 11) {
    return cpf ?? '';
  }
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

/** 2026-11-17 → 17/11/2026, sem passar por Date (evita erro de fuso em datas sem hora). */
export function dataBr(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

/** "1 dia", "3 dias" */
export function dias(n: number): string {
  return n === 1 ? '1 dia' : `${n} dias`;
}

/** Valida CPF (dígitos verificadores), como o backend. */
export function cpfValido(cpf: string): boolean {
  const d = cpf.replace(/\D/g, '');
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) {
    return false;
  }
  const digito = (base: string, peso: number) => {
    let soma = 0;
    for (const c of base) {
      soma += Number(c) * peso--;
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  const d1 = digito(d.slice(0, 9), 10);
  const d2 = digito(d.slice(0, 9) + d1, 11);
  return d.endsWith(`${d1}${d2}`);
}
