import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { By } from '@angular/platform-browser';

import { DadosIdentificacao, Identificacao, digitosTelefone, formatarTelefone } from './identificacao';

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
});
