import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Assinatura } from './assinatura';

describe('Assinatura', () => {
  let fixture: ComponentFixture<Assinatura>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Assinatura] }).compileComponents();
    fixture = TestBed.createComponent(Assinatura);
    await fixture.whenStable();
  });

  function marca(): HTMLImageElement {
    return fixture.nativeElement.querySelector('img') as HTMLImageElement;
  }

  it('usa a marca colorida da Prefeitura com o nome da biblioteca', () => {
    expect(marca().getAttribute('src')).toBe('marca/imbe-vertical.png');
    expect(marca().alt).toBe('Prefeitura de Imbé');
    expect(fixture.nativeElement.textContent).toContain('Biblioteca');
    expect(fixture.nativeElement.textContent).toContain('Municipal');
  });

  it('usa a versão branca em fundo escuro', async () => {
    fixture.componentRef.setInput('branca', true);
    await fixture.whenStable();
    expect(marca().getAttribute('src')).toBe('marca/imbe-vertical-branca.png');
  });

  it('por padrão tem altura que respeita a largura mínima de 100 px do manual', () => {
    // a vertical 1 tem proporção 283 x 240: 85 px de altura dão 100 px de largura
    expect(parseFloat(marca().style.height) * (283 / 240)).toBeGreaterThanOrEqual(100);
  });
});
