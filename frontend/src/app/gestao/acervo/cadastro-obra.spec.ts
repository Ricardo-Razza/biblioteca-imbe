import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { IsbnResposta } from '../../core/api/modelos';
import { CadastroObra } from './cadastro-obra';

describe('CadastroObra', () => {
  let fixture: ComponentFixture<CadastroObra>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CadastroObra],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(CadastroObra);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function bipar(isbn: string, resposta: IsbnResposta): Promise<void> {
    const input = fixture.nativeElement.querySelector('app-campo-isbn input') as HTMLInputElement;
    input.value = isbn;
    input.dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('app-campo-isbn form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne('/api/v1/isbn/9788535902778').flush(resposta);
    await fixture.whenStable();
  }

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('obra já cadastrada: só oferece adicionar exemplar', async () => {
    await bipar('9788535902778', {
      isbn13: '9788535902778',
      obraId: 7,
      encontrado: false,
      dados: null,
      fontes: [],
      fontesIndisponiveis: [],
    });
    expect(texto()).toContain('já está no acervo');
    const link = fixture.nativeElement.querySelector('a[href^="/painel/acervo/7"]') as HTMLAnchorElement;
    expect(link.textContent).toContain('Adicionar exemplar');
    expect(fixture.nativeElement.querySelector('app-formulario-obra')).toBeNull();
  });

  it('encontrado: preenche o formulário, salva com a fonte e abre a obra', async () => {
    await bipar('978-85-359-0277-8', {
      isbn13: '9788535902778',
      obraId: null,
      encontrado: true,
      dados: {
        titulo: 'Ensaio sobre a cegueira',
        subtitulo: null,
        autores: ['José Saramago'],
        editora: 'Companhia das Letras',
        ano: 1995,
        paginas: 312,
        idioma: 'pt',
        sinopse: null,
        assuntos: [],
        capaDisponivel: true,
        fonte: 'BRASILAPI',
      },
      fontes: ['BRASILAPI', 'OPEN_LIBRARY'],
      fontesIndisponiveis: ['GOOGLE_BOOKS'],
    });
    expect(texto()).toContain('Dados de BrasilAPI e Open Library');
    expect(texto()).toContain('Sem resposta de Google Books');
    const titulo = fixture.nativeElement.querySelector('input[formcontrolname="titulo"]') as HTMLInputElement;
    expect(titulo.value).toBe('Ensaio sobre a cegueira');
    const capa = fixture.nativeElement.querySelector('app-capa-livro img') as HTMLImageElement;
    expect(capa.getAttribute('src')).toBe('/api/v1/isbn/9788535902778/capa');

    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    (fixture.nativeElement.querySelector('app-formulario-obra form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    const req = http.expectOne('/api/v1/obras');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({
      isbn: '9788535902778',
      titulo: 'Ensaio sobre a cegueira',
      autores: ['José Saramago'],
      ano: 1995,
      fonteDados: 'BRASILAPI',
    });
    req.flush({ id: 42 });
    expect(navegar).toHaveBeenCalledWith(['/painel/acervo', 42], { queryParams: { novoExemplar: '1' } });
  });

  it('não encontrado: formulário manual, sem capa', async () => {
    await bipar('9788535902778', {
      isbn13: '9788535902778',
      obraId: null,
      encontrado: false,
      dados: null,
      fontes: [],
      fontesIndisponiveis: [],
    });
    expect(texto()).toContain('Nenhuma fonte conhece este ISBN');
    expect(fixture.nativeElement.querySelector('app-capa-livro img')).toBeNull();
    expect(texto()).toContain('Enviar foto');
  });
});
