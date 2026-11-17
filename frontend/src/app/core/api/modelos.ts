// Formatos da API (/api/v1). Espelham os records do backend.

export type FonteIsbn = 'BRASILAPI' | 'GOOGLE_BOOKS' | 'OPEN_LIBRARY' | 'MANUAL';
export type SituacaoExemplar = 'DISPONIVEL' | 'RESERVADO' | 'EMPRESTADO' | 'EXTRAVIADO' | 'BAIXADO';
export type EstadoExemplar = 'BOM' | 'DANIFICADO';
export type OrigemExemplar = 'COMPRA' | 'DOACAO';

export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  total: number;
  totalPaginas: number;
}

export interface DadosIsbn {
  titulo: string | null;
  subtitulo: string | null;
  autores: string[];
  editora: string | null;
  ano: number | null;
  paginas: number | null;
  idioma: string | null;
  sinopse: string | null;
  assuntos: string[];
  capaDisponivel: boolean;
  fonte: FonteIsbn;
}

export interface IsbnResposta {
  isbn13: string;
  /** Preenchido quando a obra já está no acervo. */
  obraId: number | null;
  encontrado: boolean;
  dados: DadosIsbn | null;
  fontes: FonteIsbn[];
  fontesIndisponiveis: FonteIsbn[];
}

export interface ObraRequisicao {
  isbn: string;
  titulo: string;
  subtitulo: string | null;
  autores: string[];
  editora: string | null;
  ano: number | null;
  paginas: number | null;
  idioma: string | null;
  sinopse: string | null;
  assuntos: string[];
  fonteDados: FonteIsbn | null;
}

export interface Obra {
  id: number;
  isbn13: string;
  titulo: string;
  subtitulo: string | null;
  autores: string[];
  editora: string | null;
  ano: number | null;
  paginas: number | null;
  idioma: string | null;
  sinopse: string | null;
  assuntos: string[];
  temCapa: boolean;
  /** Sem os baixados. */
  totalExemplares: number;
  exemplaresDisponiveis: number;
  fonteDados: FonteIsbn | null;
  criadoEm: string;
}

export interface ExemplarRequisicao {
  tombo: string;
  localizacao: string | null;
  estado: EstadoExemplar | null;
  dataAquisicao: string | null;
  origem: OrigemExemplar | null;
}

export interface ExemplarAlteracao {
  situacao?: SituacaoExemplar;
  estado?: EstadoExemplar;
  /** Em branco limpa; ausente mantém. */
  localizacao?: string;
}

export interface Exemplar {
  id: number;
  tombo: string;
  localizacao: string | null;
  estado: EstadoExemplar;
  situacao: SituacaoExemplar;
  dataAquisicao: string | null;
  origem: OrigemExemplar | null;
  criadoEm: string;
  obra: { id: number; isbn13: string; titulo: string; autores: string[] };
}

export const NOME_FONTE: Record<FonteIsbn, string> = {
  BRASILAPI: 'BrasilAPI',
  GOOGLE_BOOKS: 'Google Books',
  OPEN_LIBRARY: 'Open Library',
  MANUAL: 'Cadastro manual',
};
