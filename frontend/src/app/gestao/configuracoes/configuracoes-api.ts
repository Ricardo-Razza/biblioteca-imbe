import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, Parametro } from '../../core/api/modelos';
import { Perfil, TipoAutenticacao } from '../../core/auth/sessao';

export interface Funcionario {
  id: number;
  nome: string;
  login: string;
  tipo: TipoAutenticacao;
  perfil: Perfil;
  ativo: boolean;
  trocarSenha: boolean;
  ultimoAcesso: string | null;
  loginTravadoAte: string | null;
  criadoEm: string;
}

export interface SenhaProvisoria {
  funcionario: Funcionario;
  senhaProvisoria: string | null;
}

export interface Feriado {
  data: string;
  descricao: string;
  tipo: 'NACIONAL' | 'MUNICIPAL';
}

export interface RegistroAuditoria {
  id: number;
  dataHora: string;
  funcionarioId: number | null;
  funcionario: string | null;
  acao: string;
  descricao: string;
  entidade: string | null;
  entidadeId: number | null;
  detalhe: string | null;
}

/** Administração: funcionários, parâmetros, feriados e auditoria. */
@Injectable({ providedIn: 'root' })
export class ConfiguracoesApi {
  private readonly http = inject(HttpClient);

  funcionarios(): Observable<Funcionario[]> {
    return this.http.get<Funcionario[]>('/api/v1/funcionarios');
  }

  cadastrarFuncionario(req: { nome: string; login: string; tipo: TipoAutenticacao; perfil: Perfil }): Observable<SenhaProvisoria> {
    return this.http.post<SenhaProvisoria>('/api/v1/funcionarios', req);
  }

  alterarFuncionario(id: number, req: { nome: string; perfil: Perfil; ativo: boolean }): Observable<Funcionario> {
    return this.http.put<Funcionario>(`/api/v1/funcionarios/${id}`, req);
  }

  redefinirSenha(id: number): Observable<SenhaProvisoria> {
    return this.http.post<SenhaProvisoria>(`/api/v1/funcionarios/${id}/senha-provisoria`, null);
  }

  parametros(): Observable<Parametro[]> {
    return this.http.get<Parametro[]>('/api/v1/parametros');
  }

  alterarParametro(chave: string, valor: number): Observable<Parametro> {
    return this.http.put<Parametro>(`/api/v1/parametros/${chave}`, { valor });
  }

  feriados(ano: number): Observable<Feriado[]> {
    return this.http.get<Feriado[]>('/api/v1/feriados', { params: { ano } });
  }

  incluirFeriado(data: string, descricao: string): Observable<Feriado> {
    return this.http.post<Feriado>('/api/v1/feriados', { data, descricao });
  }

  /** Feriados nacionais do ano pela BrasilAPI. Os municipais não mudam. */
  importarFeriados(ano: number): Observable<{ ano: number; novos: number; atualizados: number }> {
    return this.http.post<{ ano: number; novos: number; atualizados: number }>('/api/v1/feriados/importacao', null, {
      params: { ano },
    });
  }

  removerFeriado(data: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/feriados/${data}`);
  }

  auditoria(acao: string | null, pagina: number, tamanho = 30): Observable<Pagina<RegistroAuditoria>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    if (acao) params = params.set('acao', acao);
    return this.http.get<Pagina<RegistroAuditoria>>('/api/v1/auditoria', { params });
  }
}
