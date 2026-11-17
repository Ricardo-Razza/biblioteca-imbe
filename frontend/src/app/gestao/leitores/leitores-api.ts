import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Leitor, LeitorRequisicao, Pagina, SituacaoLeitor } from '../../core/api/modelos';

const API = '/api/v1/leitores';

@Injectable({ providedIn: 'root' })
export class LeitoresApi {
  private readonly http = inject(HttpClient);

  listar(termo: string, pagina = 0, tamanho = 20): Observable<Pagina<Leitor>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    if (termo.trim()) {
      params = params.set('q', termo.trim());
    }
    return this.http.get<Pagina<Leitor>>(API, { params });
  }

  buscar(id: number): Observable<Leitor> {
    return this.http.get<Leitor>(`${API}/${id}`);
  }

  cadastrar(req: LeitorRequisicao): Observable<Leitor> {
    return this.http.post<Leitor>(API, req);
  }

  alterar(id: number, req: LeitorRequisicao): Observable<Leitor> {
    return this.http.put<Leitor>(`${API}/${id}`, req);
  }

  desbloquear(id: number): Observable<Leitor> {
    return this.http.post<Leitor>(`${API}/${id}/desbloqueio`, null);
  }

  situacao(id: number): Observable<SituacaoLeitor> {
    return this.http.get<SituacaoLeitor>(`${API}/${id}/situacao`);
  }
}
