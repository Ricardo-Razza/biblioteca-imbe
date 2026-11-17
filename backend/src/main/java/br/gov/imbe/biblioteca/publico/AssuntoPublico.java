package br.gov.imbe.biblioteca.publico;

/** Assunto para o filtro do catálogo, com quantas obras visíveis ele tem. */
public record AssuntoPublico(Long id, String nome, long obras) {
}
