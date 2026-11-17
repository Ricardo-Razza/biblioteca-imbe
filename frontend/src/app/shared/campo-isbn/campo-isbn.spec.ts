import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampoIsbn } from './campo-isbn';

describe('CampoIsbn', () => {
  let fixture: ComponentFixture<CampoIsbn>;
  let emitidos: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CampoIsbn] }).compileComponents();
    fixture = TestBed.createComponent(CampoIsbn);
    emitidos = [];
    fixture.componentInstance.buscar.subscribe((isbn) => emitidos.push(isbn));
    await fixture.whenStable();
  });

  function bipar(texto: string): void {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('emite o ISBN-13 quando o leitor manda o código', () => {
    bipar('8535902775');
    expect(emitidos).toEqual(['9788535902778']);
  });

  it('mostra o erro e não emite com dígito errado', async () => {
    bipar('9788535902779');
    await fixture.whenStable();
    expect(emitidos).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('dígito verificador');
  });
});
