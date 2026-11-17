package br.gov.imbe.biblioteca.acervo;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Novo exemplar de uma obra.
 *
 * @param tombo  número de tombo da etiqueta: letras, números, ponto e hífen. Vai no caminho de
 *               {@code GET /exemplares/tombo/{tombo}}, por isso sem barra. Gravado em maiúsculas.
 * @param estado sem valor, {@code BOM}
 */
public record ExemplarRequisicao(
        @NotBlank(message = "Informe o tombo.")
        @Pattern(regexp = "\\s*[A-Za-z0-9.-]{1,30}\\s*",
                message = "Use até 30 letras, números, ponto ou hífen, sem espaços nem barra.") String tombo,
        @Size(max = 100, message = "Use no máximo 100 caracteres.") String localizacao,
        EstadoExemplar estado,
        @PastOrPresent(message = "A data de aquisição não pode ser futura.") LocalDate dataAquisicao,
        OrigemExemplar origem) {
}
