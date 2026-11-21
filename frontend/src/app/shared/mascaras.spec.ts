import { FormControl } from '@angular/forms';

import { dataEntre, mascaraNomeOuCpf, mascaraTombo } from './mascaras';

describe('mascaraNomeOuCpf', () => {
  it('começando por número vira CPF com no máximo 11 números', () => {
    expect(mascaraNomeOuCpf('529982')).toBe('529.982');
    expect(mascaraNomeOuCpf('9'.repeat(34))).toBe('999.999.999-99');
    expect(mascaraNomeOuCpf('529.982.247-25')).toBe('529.982.247-25');
  });

  it('com letras fica como nome', () => {
    expect(mascaraNomeOuCpf('Ericka')).toBe('Ericka');
    expect(mascaraNomeOuCpf('Maria 2')).toBe('Maria 2');
    expect(mascaraNomeOuCpf('a'.repeat(200))).toHaveLength(150);
  });
});

describe('mascaraTombo', () => {
  it('só letras, números, ponto e hífen, até 30, em maiúsculas', () => {
    expect(mascaraTombo('bib.0042')).toBe('BIB.0042');
    expect(mascaraTombo('a b/c')).toBe('ABC');
    expect(mascaraTombo('9'.repeat(40))).toHaveLength(30);
  });
});

describe('dataEntre', () => {
  it('recusa ano fora da faixa e aceita vazio', () => {
    const v = dataEntre('1900-01-01', '2026-11-21');
    expect(v(new FormControl('0221-07-26'))).toEqual({ data: true });
    expect(v(new FormControl('2027-01-01'))).toEqual({ data: true });
    expect(v(new FormControl('1990-01-02'))).toBeNull();
    expect(v(new FormControl(''))).toBeNull();
  });
});
