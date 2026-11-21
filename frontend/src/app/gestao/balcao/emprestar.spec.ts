import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Exemplar } from '../../core/api/modelos';
import { Emprestar } from './emprestar';

function exemplar(id: number, tombo: string, titulo: string): Exemplar {
  return {
    id,
    tombo,
    localizacao: 'Estante 3',
    estado: 'BOM',
    situacao: 'DISPONIVEL',
    dataAquisicao: null,
    origem: null,
    criadoEm: '2026-11-01T10:00:00',
    obra: { id: 10 + id, isbn13: '9788535902778', titulo, autores: ['José Saramago'] },
  };
}

describe('Emprestar', () => {
  let fixture: ComponentFixture<Emprestar>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Emprestar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Emprestar);
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('leitor', '7');
    await fixture.whenStable();
    http.expectOne('/api/v1/leitores/7').flush({ id: 7, cpf: '52998224725' });
    http.expectOne('/api/v1/leitores/7/situacao').flush({
      leitorId: 7,
      nome: 'Leitora Fictícia',
      ativo: true,
      bloqueadoAte: null,
      emprestimosEmAberto: 0,
      atrasados: 0,
      reservasAguardando: 0,
      livrosEmUso: 0,
      limite: 3,
      podeEmprestar: true,
      motivos: [],
    });
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

  it('tombo exato empresta direto, como o leitor de código de barras', async () => {
    await informar('bib.0042');
    http
      .expectOne((r) => r.url === '/api/v1/exemplares' && r.params.get('q') === 'bib.0042')
      .flush([exemplar(1, 'BIB.0042', 'Ensaio sobre a cegueira')]);
    await fixture.whenStable();
    const req = http.expectOne('/api/v1/emprestimos');
    expect(req.request.body).toEqual({ leitorId: 7, tombo: 'BIB.0042' });
  });

  it('título ou autor mostra os exemplares para escolher', async () => {
    await informar('saramago');
    http
      .expectOne((r) => r.url === '/api/v1/exemplares')
      .flush([exemplar(1, 'BIB.0042', 'Ensaio sobre a cegueira'), exemplar(2, 'AUTO-000002', 'Ensaio sobre a lucidez')]);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('2 exemplares encontrados');
    expect(el.textContent).toContain('Ensaio sobre a lucidez');
    http.expectNone('/api/v1/emprestimos');

    const botoes = [...el.querySelectorAll('.linha button')] as HTMLButtonElement[];
    botoes[1].click();
    expect(http.expectOne('/api/v1/emprestimos').request.body).toEqual({ leitorId: 7, tombo: 'AUTO-000002' });
  });

  it('sem resultado avisa no campo', async () => {
    await informar('livro que não existe');
    http.expectOne((r) => r.url === '/api/v1/exemplares').flush([]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nenhum livro disponível encontrado');
  });
});
