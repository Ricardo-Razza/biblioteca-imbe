import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DadosBiblioteca } from '../core/api/modelos';
import { RodapePublico } from './rodape-publico';

describe('RodapePublico', () => {
  let fixture: ComponentFixture<RodapePublico>;

  const base: DadosBiblioteca = {
    nome: 'Biblioteca Municipal de Imbé',
    endereco: '',
    horario: '',
    telefone: '',
    email: '',
    prazoRetiradaDiasUteis: 2,
    limiteLivros: 3,
    prazoEmprestimoDias: 14,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RodapePublico], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(RodapePublico);
  });

  async function com(dados: DadosBiblioteca | null): Promise<string> {
    fixture.componentRef.setInput('biblioteca', dados);
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('explica a reserva com os prazos e o limite configurados', async () => {
    const texto = await com(base);
    expect(texto).toContain('Retire no balcão em até 2 dias úteis');
    expect(texto).toContain('até 3 livros por vez, por 14 dias úteis');
  });

  it('avisa que o contato vem em breve quando nada foi configurado', async () => {
    expect(await com(base)).toContain('Endereço e horário de atendimento em breve');
  });

  it('mostra o contato configurado, com telefone e e-mail clicáveis', async () => {
    const texto = await com({
      ...base,
      endereco: 'Rua Exemplo, 100',
      horario: 'Seg a sex, 8h às 17h',
      telefone: '(51) 3000-0000',
      email: 'biblioteca@exemplo.gov.br',
    });
    expect(texto).toContain('Rua Exemplo, 100');
    expect(texto).not.toContain('em breve');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('a[href="tel:5130000000"]')).not.toBeNull();
    expect(el.querySelector('a[href="mailto:biblioteca@exemplo.gov.br"]')).not.toBeNull();
    const mapa = el.querySelector('a.mapa') as HTMLAnchorElement;
    expect(mapa.href).toBe(
      'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent('Rua Exemplo, 100, Imbé - RS'),
    );
  });

  it('usa singular com 1 dia útil e 1 livro', async () => {
    const texto = await com({ ...base, prazoRetiradaDiasUteis: 1, limiteLivros: 1, prazoEmprestimoDias: 1 });
    expect(texto).toContain('em até 1 dia útil');
    expect(texto).toContain('até 1 livro por vez, por 1 dia útil');
  });

  it('funciona sem os dados da API', async () => {
    const texto = await com(null);
    expect(texto).toContain('Como reservar');
    expect(texto).toContain('Prefeitura Municipal de Imbé');
  });
});
