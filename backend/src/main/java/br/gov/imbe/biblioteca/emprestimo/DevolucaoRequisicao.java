package br.gov.imbe.biblioteca.emprestimo;

import jakarta.validation.constraints.Size;

/** Corpo opcional da devolução. Sem ocorrência: o livro voltou em ordem. */
public record DevolucaoRequisicao(
        OcorrenciaDevolucao ocorrencia,
        @Size(max = 500, message = "Use no máximo 500 caracteres.") String observacao) {
}
