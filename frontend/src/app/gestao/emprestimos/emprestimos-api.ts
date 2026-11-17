import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Emprestimo, OcorrenciaDevolucao, Pagina, StatusEmprestimo } from '../../core/api/modelos';

const API = '/api/v1/emprestimos';

export interface FiltroEmprestimos {
  status?: StatusEmprestimo;
  leitorId?: number;
  tombo?: string;
  q?: string;
  pagina?: number;
  tamanho?: number;
}

@Injectable({ providedIn: 'root' })
export class EmprestimosApi {
  private readonly http = inject(HttpClient);

  listar(f: FiltroEmprestimos): Observable<Pagina<Emprestimo>> {
    let params = new HttpParams().set('page', f.pagina ?? 0).set('size', f.tamanho ?? 20);
    if (f.status) params = params.set('status', f.status);
    if (f.leitorId != null) params = params.set('leitorId', f.leitorId);
    if (f.tombo?.trim()) params = params.set('tombo', f.tombo.trim());
    if (f.q?.trim()) params = params.set('q', f.q.trim());
    return this.http.get<Pagina<Emprestimo>>(API, { params });
  }

  emprestar(leitorId: number, tombo: string): Observable<Emprestimo> {
    return this.http.post<Emprestimo>(API, { leitorId, tombo });
  }

  renovar(id: number): Observable<Emprestimo> {
    return this.http.post<Emprestimo>(`${API}/${id}/renovacao`, null);
  }

  devolver(id: number, ocorrencia: OcorrenciaDevolucao | null, observacao: string | null): Observable<Emprestimo> {
    return this.http.post<Emprestimo>(`${API}/${id}/devolucao`, { ocorrencia, observacao });
  }
}
