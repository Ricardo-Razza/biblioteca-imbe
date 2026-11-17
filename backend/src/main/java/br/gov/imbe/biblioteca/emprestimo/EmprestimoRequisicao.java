package br.gov.imbe.biblioteca.emprestimo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Saída no balcão: o leitor e o tombo bipado. */
public record EmprestimoRequisicao(
        @NotNull(message = "Escolha o leitor.") Long leitorId,
        @NotBlank(message = "Informe o tombo.") String tombo) {
}
