import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';

export type Perfil = 'ADMIN' | 'ATENDENTE';
export type TipoAutenticacao = 'AD' | 'LOCAL';

export interface FuncionarioLogado {
  id: number;
  nome: string;
  login: string;
  tipo: TipoAutenticacao;
  perfil: Perfil;
  trocarSenha: boolean;
}

interface LoginResposta {
  token: string;
  expiraEm: string;
  funcionario: FuncionarioLogado;
}

const CHAVE = 'biblioteca.sessao';

interface Guardado {
  token: string;
  expiraEm: string;
}

/**
 * Sessão do funcionário. O token fica no sessionStorage: fechar o navegador do balcão encerra a sessão.
 * O perfil e a troca de senha vêm sempre do backend (`/auth/eu`), não do token.
 */
@Injectable({ providedIn: 'root' })
export class Sessao {
  private readonly http = inject(HttpClient);
  private readonly guardado = signal<Guardado | null>(ler());

  readonly funcionario = signal<FuncionarioLogado | null>(null);
  readonly token = computed(() => this.guardado()?.token ?? null);
  readonly admin = computed(() => this.funcionario()?.perfil === 'ADMIN');

  /** Tem token ainda dentro da validade (não garante que o backend aceite). */
  temToken(): boolean {
    const g = this.guardado();
    return !!g && new Date(g.expiraEm).getTime() > Date.now();
  }

  entrar(login: string, senha: string): Observable<FuncionarioLogado> {
    return this.http.post<LoginResposta>('/api/v1/auth/login', { login, senha }).pipe(
      tap((r) => {
        this.guardar({ token: r.token, expiraEm: r.expiraEm });
        this.funcionario.set(r.funcionario);
      }),
      map((r) => r.funcionario),
    );
  }

  /** Carrega quem está logado (ao abrir o app com token guardado). Falha: sai. */
  carregar(): Observable<FuncionarioLogado | null> {
    if (this.funcionario()) {
      return of(this.funcionario());
    }
    if (!this.temToken()) {
      this.sair();
      return of(null);
    }
    return this.http.get<FuncionarioLogado>('/api/v1/auth/eu').pipe(
      tap((f) => this.funcionario.set(f)),
      catchError(() => {
        this.sair();
        return of(null);
      }),
    );
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<FuncionarioLogado> {
    return this.http
      .post<FuncionarioLogado>('/api/v1/auth/senha', { senhaAtual, novaSenha })
      .pipe(tap((f) => this.funcionario.set(f)));
  }

  sair(): void {
    this.guardar(null);
    this.funcionario.set(null);
  }

  private guardar(g: Guardado | null): void {
    this.guardado.set(g);
    try {
      if (g) {
        sessionStorage.setItem(CHAVE, JSON.stringify(g));
      } else {
        sessionStorage.removeItem(CHAVE);
      }
    } catch {
      // Armazenamento bloqueado: a sessão vale só enquanto a página estiver aberta.
    }
  }
}

function ler(): Guardado | null {
  try {
    const texto = sessionStorage.getItem(CHAVE);
    const g = texto ? (JSON.parse(texto) as Guardado) : null;
    return g?.token && g.expiraEm ? g : null;
  } catch {
    return null;
  }
}
