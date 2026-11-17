import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina } from '../../../core/api/modelos';
import { TomSelo } from '../../../shared/selo/selo';

const API = '/api/v1/importacoes';

export type ResultadoItem =
  | 'PENDENTE'
  | 'CADASTRADO'
  | 'EXEMPLAR_ADICIONADO'
  | 'JA_CADASTRADO'
  | 'NAO_ENCONTRADO'
  | 'FONTE_INDISPONIVEL'
  | 'ERRO';

export interface Importacao {
  id: number;
  arquivo: string;
  funcionario: string | null;
  criadaEm: string;
  status: 'PROCESSANDO' | 'CONCLUIDA';
  finalizadaEm: string | null;
  total: number;
  pendentes: number;
  contagem: Record<ResultadoItem, number>;
}

export interface ItemImportacao {
  id: number;
  linha: number;
  isbnOriginal: string;
  isbn13: string | null;
  tombo: string | null;
  localizacao: string | null;
  resultado: ResultadoItem;
  obraId: number | null;
  titulo: string | null;
  mensagem: string | null;
}

/** Ordem e texto dos resultados na tela, com o tom do selo. */
export const RESULTADOS: { valor: ResultadoItem; texto: string; tom: TomSelo }[] = [
  { valor: 'CADASTRADO', texto: 'Cadastrados', tom: 'ok' },
  { valor: 'EXEMPLAR_ADICIONADO', texto: 'Exemplar incluído', tom: 'ok' },
  { valor: 'JA_CADASTRADO', texto: 'Já no acervo', tom: 'neutra' },
  { valor: 'NAO_ENCONTRADO', texto: 'Não encontrados', tom: 'alerta' },
  { valor: 'FONTE_INDISPONIVEL', texto: 'Fonte fora do ar', tom: 'alerta' },
  { valor: 'ERRO', texto: 'Com erro', tom: 'erro' },
  { valor: 'PENDENTE', texto: 'Na fila', tom: 'neutra' },
];

export function textoResultado(r: ResultadoItem): { texto: string; tom: TomSelo } {
  const singular: Record<ResultadoItem, string> = {
    PENDENTE: 'Na fila',
    CADASTRADO: 'Cadastrado',
    EXEMPLAR_ADICIONADO: 'Exemplar incluído',
    JA_CADASTRADO: 'Já no acervo',
    NAO_ENCONTRADO: 'Não encontrado',
    FONTE_INDISPONIVEL: 'Fonte fora do ar',
    ERRO: 'Erro',
  };
  return { texto: singular[r], tom: RESULTADOS.find((x) => x.valor === r)!.tom };
}

/** Porcentagem já processada (0 a 100). */
export function progresso(i: Pick<Importacao, 'total' | 'pendentes'>): number {
  return i.total === 0 ? 100 : Math.round(((i.total - i.pendentes) / i.total) * 100);
}

@Injectable({ providedIn: 'root' })
export class ImportacaoApi {
  private readonly http = inject(HttpClient);

  enviar(arquivo: File): Observable<Importacao> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo, arquivo.name);
    return this.http.post<Importacao>(API, corpo);
  }

  listar(pagina = 0): Observable<Pagina<Importacao>> {
    return this.http.get<Pagina<Importacao>>(API, { params: new HttpParams().set('page', pagina).set('size', 10) });
  }

  buscar(id: number): Observable<Importacao> {
    return this.http.get<Importacao>(`${API}/${id}`);
  }

  itens(id: number, resultado: ResultadoItem | null, pagina: number): Observable<Pagina<ItemImportacao>> {
    let params = new HttpParams().set('page', pagina).set('size', 50);
    if (resultado) params = params.set('resultado', resultado);
    return this.http.get<Pagina<ItemImportacao>>(`${API}/${id}/itens`, { params });
  }

  tentarDeNovo(id: number): Observable<Importacao> {
    return this.http.post<Importacao>(`${API}/${id}/nova-tentativa`, null);
  }
}
