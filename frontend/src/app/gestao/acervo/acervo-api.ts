import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Exemplar,
  ExemplarAlteracao,
  ExemplarRequisicao,
  IsbnResposta,
  Obra,
  ObraRequisicao,
  Pagina,
} from '../../core/api/modelos';

const API = '/api/v1';

@Injectable({ providedIn: 'root' })
export class AcervoApi {
  private readonly http = inject(HttpClient);

  buscarIsbn(isbn: string): Observable<IsbnResposta> {
    return this.http.get<IsbnResposta>(`${API}/isbn/${encodeURIComponent(isbn)}`);
  }

  /** Prévia da capa indicada pelas fontes, servida pelo backend. */
  previaCapaIsbn(isbn13: string): string {
    return `${API}/isbn/${isbn13}/capa`;
  }

  listarObras(termo: string, pagina: number, tamanho: number, semCapa = false): Observable<Pagina<Obra>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    if (termo.trim()) {
      params = params.set('q', termo.trim());
    }
    if (semCapa) {
      params = params.set('semCapa', true);
    }
    return this.http.get<Pagina<Obra>>(`${API}/obras`, { params });
  }

  obra(id: number): Observable<Obra> {
    return this.http.get<Obra>(`${API}/obras/${id}`);
  }

  cadastrarObra(req: ObraRequisicao): Observable<Obra> {
    return this.http.post<Obra>(`${API}/obras`, req);
  }

  alterarObra(id: number, req: ObraRequisicao): Observable<Obra> {
    return this.http.put<Obra>(`${API}/obras/${id}`, req);
  }

  enviarCapa(id: number, arquivo: File): Observable<Obra> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);
    return this.http.post<Obra>(`${API}/obras/${id}/capa`, corpo);
  }

  exemplares(obraId: number): Observable<Exemplar[]> {
    return this.http.get<Exemplar[]>(`${API}/obras/${obraId}/exemplares`);
  }

  cadastrarExemplar(obraId: number, req: ExemplarRequisicao): Observable<Exemplar> {
    return this.http.post<Exemplar>(`${API}/obras/${obraId}/exemplares`, req);
  }

  alterarExemplar(id: number, alt: ExemplarAlteracao): Observable<Exemplar> {
    return this.http.patch<Exemplar>(`${API}/exemplares/${id}`, alt);
  }
}
