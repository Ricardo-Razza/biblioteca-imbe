import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina, Reserva, StatusReserva } from '../../core/api/modelos';
import { TomSelo } from '../../shared/selo/selo';
import { dataBr } from '../../shared/formatos';

const API = '/api/v1/reservas';

export const FINALIZADAS: StatusReserva[] = ['RETIRADA', 'CANCELADA', 'EXPIRADA'];

export interface FiltroReservas {
  status?: StatusReserva[];
  leitorId?: number;
  q?: string;
  pagina?: number;
  tamanho?: number;
}

@Injectable({ providedIn: 'root' })
export class ReservasApi {
  private readonly http = inject(HttpClient);

  listar(f: FiltroReservas): Observable<Pagina<Reserva>> {
    let params = new HttpParams().set('page', f.pagina ?? 0).set('size', f.tamanho ?? 20);
    for (const s of f.status ?? []) params = params.append('status', s);
    if (f.leitorId != null) params = params.set('leitorId', f.leitorId);
    if (f.q?.trim()) params = params.set('q', f.q.trim());
    return this.http.get<Pagina<Reserva>>(API, { params });
  }

  entregar(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${API}/${id}/retirada`, null);
  }

  cancelar(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${API}/${id}/cancelamento`, null);
  }
}

/** "Retirar até 19/11", "Último dia hoje", "Prazo vencido em 18/11", "Retirada em ...". */
export function seloReserva(r: Reserva, hoje: string): { texto: string; tom: TomSelo } {
  switch (r.status) {
    case 'AGUARDANDO_RETIRADA':
      if (r.prazoVencido) return { texto: `Prazo vencido em ${dataBr(r.prazoRetirada)}`, tom: 'erro' };
      if (r.prazoRetirada === hoje) return { texto: 'Último dia hoje', tom: 'alerta' };
      return { texto: `Retirar até ${dataBr(r.prazoRetirada)}`, tom: 'ok' };
    case 'RETIRADA':
      return { texto: `Entregue em ${dataBr(r.finalizadaEm)}`, tom: 'neutra' };
    case 'CANCELADA':
      return {
        texto: `Cancelada ${r.canceladaPor === 'LEITOR' ? 'pelo leitor' : 'pela Sala Literária'} em ${dataBr(r.finalizadaEm)}`,
        tom: 'neutra',
      };
    case 'EXPIRADA':
      return { texto: `Expirada em ${dataBr(r.finalizadaEm)}`, tom: 'alerta' };
  }
}

export function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
