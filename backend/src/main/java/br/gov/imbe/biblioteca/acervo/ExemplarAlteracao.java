package br.gov.imbe.biblioteca.acervo;

import jakarta.validation.constraints.Size;

/**
 * Alteração parcial de exemplar. Campo ausente fica como está; {@code localizacao} em branco limpa o valor.
 * Tombo, origem e data de aquisição não mudam.
 *
 * @param situacao só {@code EXTRAVIADO}, {@code BAIXADO} ou, de extraviado, {@code DISPONIVEL}
 */
public record ExemplarAlteracao(
        SituacaoExemplar situacao,
        EstadoExemplar estado,
        @Size(max = 100, message = "Use no máximo 100 caracteres.") String localizacao) {
}
