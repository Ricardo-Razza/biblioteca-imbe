package br.gov.imbe.biblioteca.comum.parametro;

/** Parâmetros numéricos, com os limites aceitos na edição. Os valores iniciais estão na migration V2. */
public enum ChaveParametro {
    PRAZO_EMPRESTIMO_DIAS(1, 90),
    LIMITE_LIVROS_POR_LEITOR(1, 20),
    MAXIMO_RENOVACOES(0, 10),
    DIAS_POR_RENOVACAO(1, 90),
    PRAZO_RETIRADA_RESERVA_DIAS_UTEIS(1, 10);

    private final int minimo;
    private final int maximo;

    ChaveParametro(int minimo, int maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public int minimo() {
        return minimo;
    }

    public int maximo() {
        return maximo;
    }
}
