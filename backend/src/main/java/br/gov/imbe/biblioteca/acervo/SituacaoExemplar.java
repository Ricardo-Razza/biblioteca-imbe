package br.gov.imbe.biblioteca.acervo;

/**
 * Onde o exemplar está. {@code RESERVADO} e {@code EMPRESTADO} são controlados pela reserva e pelo empréstimo;
 * manualmente só se marca extravio, baixa ou a volta de um extraviado. {@code BAIXADO} é definitivo.
 */
public enum SituacaoExemplar {
    DISPONIVEL,
    RESERVADO,
    EMPRESTADO,
    EXTRAVIADO,
    BAIXADO
}
