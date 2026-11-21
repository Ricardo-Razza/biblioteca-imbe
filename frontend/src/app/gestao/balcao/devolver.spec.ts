import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Emprestimo } from '../../core/api/modelos';
import { Devolver } from './devolver';

function emprestimo(id: number, tombo: string, titulo: string, leitor: string): Emprestimo {
  return {
    id,
    exemplar: { id, tombo, obraId: 10 + id, titulo, autores: ['José Saramago'], obraTemCapa: false },
    leitor: { id: 7, nome: leitor },
    dataSaida: '2026-11-10',
    dataPrevista: '2026-11-24',
    dataDevolucao: null,
    renovacoes: 0,
    renovacoesRestantes: 2,
    diasAtraso: 0,
    status: 'ATIVO',
  } as Emprestimo;
}

const pagina = (itens: Emprestimo[]) => ({ itens, total: itens.length, pagina: 0, tamanho: 20 });

describe('Devolver', () => {
  let fixture: ComponentFixture<Devolver>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Devolver],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Devolver);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function informar(texto: string): Promise<void> {
    const el = fixture.nativeElement as HTMLElement;
    const campo = el.querySelector('form.tombo input') as HTMLInputElement;
    campo.value = texto;
    campo.dispatchEvent(new Event('input'));
    (el.querySelector('form.tombo') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('tombo exato abre a conferência direto', async () => {
    await informar('bib.0042');
    const req = http.expectOne((r) => r.url === '/api/v1/emprestimos');
    expect(req.request.params.get('q')).toBe('bib.0042');
    expect(req.request.params.get('status')).toBe('ATIVO');
    req.flush(pagina([emprestimo(1, 'BIB.0042', 'Ensaio sobre a cegueira', 'Carla'), emprestimo(2, 'BIB.00421', 'Outro', 'Ana')]));
    await fixture.whenStable();
    expect(texto()).toContain('Confirmar devolução');
    expect(texto()).not.toContain('encontrados');
  });

  it('título, autor ou leitor com vários resultados mostra a lista para escolher', async () => {
    await informar('saramago');
    http
      .expectOne((r) => r.url === '/api/v1/emprestimos')
      .flush(pagina([emprestimo(1, 'AUTO-000001', 'Ensaio sobre a cegueira', 'Carla'), emprestimo(2, 'AUTO-000002', 'Ensaio sobre a lucidez', 'Ana')]));
    await fixture.whenStable();
    expect(texto()).toContain('2 empréstimos em aberto encontrados');
    expect(texto()).not.toContain('Confirmar devolução');

    const botao = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.encontrados button')].find((b) =>
      b.textContent?.includes('Devolver'),
    ) as HTMLButtonElement;
    botao.click();
    await fixture.whenStable();
    expect(texto()).toContain('Confirmar devolução');
    expect(texto()).not.toContain('encontrados');
  });

  it('sem resultado avisa no campo', async () => {
    await informar('livro que não existe');
    http.expectOne((r) => r.url === '/api/v1/emprestimos').flush(pagina([]));
    await fixture.whenStable();
    expect(texto()).toContain('Nenhum empréstimo em aberto encontrado');
  });
});
