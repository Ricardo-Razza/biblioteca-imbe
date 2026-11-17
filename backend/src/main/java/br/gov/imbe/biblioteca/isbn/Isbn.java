package br.gov.imbe.biblioteca.isbn;

import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/**
 * ISBN já validado e sempre no formato ISBN-13 (só dígitos).
 * Aceita o que vem do leitor de código de barras (EAN-13 978/979) ou digitado, com ou sem hífens e espaços.
 * ISBN-10 é convertido para ISBN-13 com prefixo 978.
 */
public record Isbn(String valor) {

    private static final String TITULO_ERRO = "ISBN inválido";

    public Isbn {
        if (valor == null || !valor.matches("97[89]\\d{10}") || digitoIsbn13(valor) != valor.charAt(12) - '0') {
            throw new IllegalArgumentException("Use Isbn.de(...) para validar a entrada: " + valor);
        }
    }

    /** Valida e normaliza a entrada. Lança {@link RequisicaoInvalidaException} (400) com o motivo em português. */
    public static Isbn de(String entrada) {
        String limpo = entrada == null ? "" : entrada.replaceAll("[\\s-]", "").toUpperCase();
        if (limpo.isEmpty()) {
            throw invalido("Informe o ISBN.");
        }
        return switch (limpo.length()) {
            case 13 -> deIsbn13(limpo);
            case 10 -> deIsbn10(limpo);
            default -> throw invalido("O ISBN deve ter 10 ou 13 dígitos.");
        };
    }

    private static Isbn deIsbn13(String isbn) {
        if (!isbn.matches("\\d{13}")) {
            throw invalido("O ISBN-13 deve ter só números.");
        }
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
            throw invalido("O código lido não é um ISBN: o ISBN-13 começa com 978 ou 979.");
        }
        if (digitoIsbn13(isbn) != isbn.charAt(12) - '0') {
            throw invalido("O dígito verificador não confere. Confira o número.");
        }
        return new Isbn(isbn);
    }

    private static Isbn deIsbn10(String isbn) {
        if (!isbn.matches("\\d{9}[\\dX]")) {
            throw invalido("O ISBN-10 deve ter 9 números e terminar com número ou X.");
        }
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (isbn.charAt(i) - '0') * (10 - i);
        }
        int informado = isbn.charAt(9) == 'X' ? 10 : isbn.charAt(9) - '0';
        if ((soma + informado) % 11 != 0) {
            throw invalido("O dígito verificador não confere. Confira o número.");
        }
        String base = "978" + isbn.substring(0, 9);
        return new Isbn(base + digitoIsbn13(base));
    }

    /** Dígito verificador do ISBN-13 (pesos 1 e 3 alternados) calculado sobre os 12 primeiros dígitos. */
    private static int digitoIsbn13(String isbn) {
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - soma % 10) % 10;
    }

    private static RequisicaoInvalidaException invalido(String mensagem) {
        return new RequisicaoInvalidaException(TITULO_ERRO, mensagem);
    }

    @Override
    public String toString() {
        return valor;
    }
}
