import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { FormControl } from '@angular/forms';
import { senhaForte } from '../../gestao/conta/troca-senha';
import { interceptorAuth } from './interceptor-auth';
import { Sessao } from './sessao';

describe('interceptorAuth', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let sessao: Sessao;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([interceptorAuth])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(Sessao);
  });

  afterEach(() => backend.verify());

  function entrar(): void {
    sessao.entrar('maria', 'segredo').subscribe();
    backend.expectOne('/api/v1/auth/login').flush({
      token: 'abc.def.ghi',
      expiraEm: new Date(Date.now() + 3_600_000).toISOString(),
      funcionario: { id: 1, nome: 'Maria', login: 'maria', tipo: 'LOCAL', perfil: 'ATENDENTE', trocarSenha: false },
    });
  }

  it('manda o token na gestão e não no site público nem no login', () => {
    entrar();
    http.get('/api/v1/leitores').subscribe();
    expect(backend.expectOne('/api/v1/leitores').request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');

    http.get('/api/v1/publico/obras').subscribe();
    expect(backend.expectOne('/api/v1/publico/obras').request.headers.has('Authorization')).toBe(false);
  });

  it('401 encerra a sessão e volta para o login', () => {
    entrar();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http.get('/api/v1/leitores').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/leitores').flush({ detail: 'Entre no sistema para continuar.' }, { status: 401, statusText: 'Unauthorized' });

    expect(sessao.token()).toBeNull();
    expect(sessao.funcionario()).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'], expect.objectContaining({ queryParams: expect.objectContaining({ expirou: 1 }) }));
  });

  it('senha errada no login não é tratada como sessão expirada', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate');
    sessao.entrar('maria', 'errada').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/auth/login').flush({ detail: 'Login ou senha incorretos.' }, { status: 401, statusText: 'Unauthorized' });
    expect(navegar).not.toHaveBeenCalled();
  });
});

describe('senhaForte', () => {
  const erro = (v: string) => senhaForte(new FormControl(v, { nonNullable: true }));

  it('segue as regras do backend', () => {
    expect(erro('curta1')).toEqual({ senha: 'Use pelo menos 8 caracteres.' });
    expect(erro('somenteletras')).toEqual({ senha: 'Use letras e números.' });
    expect(erro('12345678')).toEqual({ senha: 'Use letras e números.' });
    expect(erro('Biblioteca2026')).toBeNull();
    expect(erro('')).toBeNull();
  });
});
