package br.gov.imbe.biblioteca.relatorio;

import java.util.Arrays;
import java.util.Optional;

/** Relatórios em CSV. {@code periodo}: aceita {@code de} e {@code ate}. */
enum TipoRelatorio {
    EMPRESTIMOS("emprestimos", "Empréstimos", true),
    ATRASADOS("atrasados", "Empréstimos atrasados", false),
    ACERVO("acervo", "Acervo", false),
    MAIS_EMPRESTADOS("mais-emprestados", "Livros mais emprestados", true),
    RESERVAS("reservas", "Reservas", true),
    BUSCAS_SEM_RESULTADO("buscas-sem-resultado", "Buscas sem resultado no site", true);

    final String caminho;
    final String nome;
    final boolean periodo;

    TipoRelatorio(String caminho, String nome, boolean periodo) {
        this.caminho = caminho;
        this.nome = nome;
        this.periodo = periodo;
    }

    static Optional<TipoRelatorio> doCaminho(String caminho) {
        return Arrays.stream(values()).filter(t -> t.caminho.equals(caminho)).findFirst();
    }
}
