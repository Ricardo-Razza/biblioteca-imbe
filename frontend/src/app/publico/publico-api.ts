import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import {
  AssuntoPublico,
  DadosBiblioteca,
  MinhasReservas,
  ObraPublica,
  ObraPublicaDetalhe,
  Pagina,
  ReservaPublica,
} from '../core/api/modelos';

const API = '/api/v1/publico';

export interface FiltroCatalogo {
  q?: string | null;
  assunto?: number | null;
  disponiveis?: boolean;
  pagina?: number;
  tamanho?: number;
}

/** Site público. CPF e data de nascimento vão só no corpo do POST, nunca na URL. */
@Injectable({ providedIn: 'root' })
export class PublicoApi {
  private readonly http = inject(HttpClient);
  private biblioteca$?: Observable<DadosBiblioteca>;

  /** Pedido uma vez por visita: rodapé e reserva usam os mesmos dados. */
  biblioteca(): Observable<DadosBiblioteca> {
    this.biblioteca$ ??= this.http.get<DadosBiblioteca>(`${API}/biblioteca`).pipe(shareReplay(1));
    return this.biblioteca$;
  }

  obras(f: FiltroCatalogo): Observable<Pagina<ObraPublica>> {
    let params = new HttpParams().set('page', f.pagina ?? 0).set('size', f.tamanho ?? 24);
    if (f.q?.trim()) params = params.set('q', f.q.trim());
    if (f.assunto != null) params = params.set('assunto', f.assunto);
    if (f.disponiveis) params = params.set('disponiveis', true);
    return this.http.get<Pagina<ObraPublica>>(`${API}/obras`, { params });
  }

  obra(id: number): Observable<ObraPublicaDetalhe> {
    return this.http.get<ObraPublicaDetalhe>(`${API}/obras/${id}`);
  }

  assuntos(): Observable<AssuntoPublico[]> {
    return this.http.get<AssuntoPublico[]>(`${API}/assuntos`);
  }

  reservar(
    obraId: number,
    cpf: string,
    dataNascimento: string,
    telefone: string,
    email: string | null,
  ): Observable<ReservaPublica> {
    return this.http.post<ReservaPublica>(`${API}/reservas`, { obraId, cpf, dataNascimento, telefone, email });
  }

  minhasReservas(cpf: string, dataNascimento: string): Observable<MinhasReservas> {
    return this.http.post<MinhasReservas>(`${API}/reservas/consulta`, { cpf, dataNascimento });
  }

  cancelar(protocolo: string, cpf: string, dataNascimento: string): Observable<ReservaPublica> {
    return this.http.post<ReservaPublica>(`${API}/reservas/${encodeURIComponent(protocolo)}/cancelamento`, {
      cpf,
      dataNascimento,
    });
  }
}
