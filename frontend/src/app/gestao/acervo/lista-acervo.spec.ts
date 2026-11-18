import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { ListaAcervo } from './lista-acervo';

const VAZIA = { itens: [], total: 0, pagina: 0, tamanho: 20 };

describe('ListaAcervo', () => {
  let fixture: ComponentFixture<ListaAcervo>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaAcervo],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ListaAcervo);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function botaoSemCapa(): HTMLButtonElement {
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Sem capa'),
    ) as HTMLButtonElement;
  }

  it('sem o filtro não manda semCapa; o botão liga o filtro na URL', async () => {
    await fixture.whenStable();
    const req = http.expectOne((r) => r.url === '/api/v1/obras');
    expect(req.request.params.has('semCapa')).toBe(false);
    req.flush(VAZIA);
    await fixture.whenStable();

    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    botaoSemCapa().click();
    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: { q: null, pagina: null, semCapa: 1 },
    }));
  });

  it('com ?semCapa=1 pede só as obras sem capa e mostra o filtro ligado', async () => {
    fixture.componentRef.setInput('semCapa', '1');
    await fixture.whenStable();
    const req = http.expectOne((r) => r.url === '/api/v1/obras');
    expect(req.request.params.get('semCapa')).toBe('true');
    req.flush(VAZIA);
    await fixture.whenStable();

    expect(botaoSemCapa().getAttribute('aria-pressed')).toBe('true');
    expect(fixture.nativeElement.textContent).toContain('Todas as obras têm capa.');
  });
});
