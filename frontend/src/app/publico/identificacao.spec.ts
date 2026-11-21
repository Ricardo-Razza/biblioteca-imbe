import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { By } from '@angular/platform-browser';

import { digitosTelefone, formatarTelefone, mascaraCpf, mascaraTelefone } from '../shared/mascaras';
import { DadosIdentificacao, Identificacao } from './identificacao';

/** CPF fictício com dígitos verificadores válidos. */
const CPF = '529.982.247-25';

describe('telefone', () => {
  it('aceita DDD com 8 ou 9 dígitos e tira o 55', () => {
    expect(digitosTelefone('(51) 99999-0000')).toBe('51999990000');
    expect(digitosTelefone('+55 51 3333-4444')).toBe('5133334444');
    expect(digitosTelefone('9999-0000')).toBeNull();
    expect(formatarTelefone('51999990000')).toBe('(51) 99999-0000');
    expect(formatarTelefone('5133334444')).toBe('(51) 3333-4444');
    expect(formatarTelefone('123')).toBe('123');
  });
});

describe('máscaras', () => {
  it('CPF: só números, no máximo 11, com pontos e traço enquanto digita', () => {
    expect(mascaraCpf('529')).toBe('529');
    expect(mascaraCpf('5299')).toBe('529.9');
    expect(mascaraCpf('5299822')).toBe('529.982.2');
    expect(mascaraCpf('5299822472')).toBe('529.982.247-2');
    expect(mascaraCpf('52998224725')).toBe(CPF);
    expect(mascaraCpf('2'.repeat(44))).toBe('222.222.222-22');
    expect(mascaraCpf('abc')).toBe('');
  });

  it('telefone: DDD entre parênteses, no máximo 11 números', () => {
    expect(mascaraTelefone('5')).toBe('(5');
    expect(mascaraTelefone('5199')).toBe('(51) 99');
    expect(mascaraTelefone('5133334444')).toBe('(51) 3333-4444');
    expect(mascaraTelefone('51999990000')).toBe('(51) 99999-0000');
    expect(mascaraTelefone('+55 51 99999-0000')).toBe('(51) 99999-0000');
    expect(mascaraTelefone('2'.repeat(34))).toBe('(22) 22222-2222');
  });
});

describe('Identificacao', () => {
  let fixture: ComponentFixture<Identificacao>;
  let enviados: DadosIdentificacao[];

  function montar(pedirContato: boolean): void {
    TestBed.configureTestingModule({ imports: [Identificacao], providers: [provideZonelessChangeDetection()] });
    fixture = TestBed.createComponent(Identificacao);
    fixture.componentRef.setInput('pedirContato', pedirContato);
    enviados = [];
    fixture.componentInstance.enviado.subscribe((d) => enviados.push(d));
    fixture.detectChanges();
  }

  function preencher(seletor: string, valor: string): void {
    const el = fixture.debugElement.query(By.css(seletor)).nativeElement as HTMLInputElement;
    el.value = valor;
    el.dispatchEvent(new Event('input'));
  }

  function enviar(): void {
    (fixture.debugElement.query(By.css('form')).nativeElement as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('sem contato não mostra telefone nem e-mail', () => {
    montar(false);
    expect(fixture.debugElement.query(By.css('input[type=tel]'))).toBeNull();
    preencher('input[formControlName=cpf]', CPF);
    preencher('input[type=date]', '1990-01-02');
    enviar();
    expect(enviados).toEqual([{ cpf: '52998224725', dataNascimento: '1990-01-02' }]);
  });

  it('sem contato não mostra o nome', () => {
    montar(false);
    expect(fixture.debugElement.query(By.css('input[formControlName=nome]'))).toBeNull();
  });

  it('na reserva exige nome e telefone e manda o e-mail só se preenchido', () => {
    montar(true);
    preencher('input[formControlName=cpf]', CPF);
    preencher('input[type=date]', '1990-01-02');
    preencher('input[type=tel]', '51 99999 0000');
    enviar();
    expect(enviados).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Informe o nome completo.');

    preencher('input[formControlName=nome]', '  Leitora   Fictícia ');
    enviar();
    expect(enviados).toEqual([
      {
        nome: 'Leitora Fictícia',
        cpf: '52998224725',
        dataNascimento: '1990-01-02',
        telefone: '(51) 99999-0000',
        email: null,
      },
    ]);

    preencher('input[type=email]', ' leitor@exemplo.com ');
    enviar();
    expect(enviados[1].email).toBe('leitor@exemplo.com');
  });

  it('recusa telefone sem DDD', () => {
    montar(true);
    preencher('input[formControlName=nome]', 'Leitora Fictícia');
    preencher('input[formControlName=cpf]', CPF);
    preencher('input[type=date]', '1990-01-02');
    preencher('input[type=tel]', '9999-0000');
    enviar();
    expect(enviados).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Telefone inválido');
  });

  it('recusa nascimento antes de 1900 ou no futuro', () => {
    montar(false);
    preencher('input[formControlName=cpf]', CPF);
    preencher('input[type=date]', '0221-07-26');
    enviar();
    expect(enviados).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Data inválida');
    preencher('input[type=date]', '2999-01-01');
    enviar();
    expect(enviados).toEqual([]);
  });

  it('CPF digitado ganha a máscara e não passa de 11 números', () => {
    montar(false);
    preencher('input[formControlName=cpf]', '2'.repeat(44));
    const el = fixture.debugElement.query(By.css('input[formControlName=cpf]')).nativeElement as HTMLInputElement;
    fixture.detectChanges();
    expect(el.value).toBe('222.222.222-22');
  });
});
