package br.gov.imbe.biblioteca.emprestimo;

/** Registrada na devolução quando o livro não volta em ordem. */
public enum OcorrenciaDevolucao {
    /** Voltou estragado: o exemplar fica marcado como danificado. */
    DANIFICADO,
    /** O leitor perdeu o livro: o exemplar fica extraviado. */
    EXTRAVIADO
}
