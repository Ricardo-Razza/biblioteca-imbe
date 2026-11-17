package br.gov.imbe.biblioteca.isbn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

class IsbnTest {

    @ParameterizedTest
    @CsvSource({
            "9788545702870,     9788545702870",
            "978-85-457-0287-0, 9788545702870",
            "' 978 85 359 0277 8 ', 9788535902778",
            "9791032305690,     9791032305690",
            "0306406152,        9780306406157",
            "0-306-40615-2,     9780306406157",
            "080442957X,        9780804429573",
            "080442957x,        9780804429573"
    })
    void aceitaEConverteParaIsbn13(String entrada, String esperado) {
        assertThat(Isbn.de(entrada).valor()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "9788545702871, O dígito verificador não confere. Confira o número.",
            "0306406153,    O dígito verificador não confere. Confira o número.",
            "7891234567895, 'O código lido não é um ISBN: o ISBN-13 começa com 978 ou 979.'",
            "97885457028A0, O ISBN-13 deve ter só números.",
            "03064X6152,    O ISBN-10 deve ter 9 números e terminar com número ou X.",
            "12345,         O ISBN deve ter 10 ou 13 dígitos.",
            "'   ',         Informe o ISBN."
    })
    void recusaComMotivoEmPortugues(String entrada, String motivo) {
        assertThatThrownBy(() -> Isbn.de(entrada))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage(motivo);
    }

    @Test
    void recusaNulo() {
        assertThatThrownBy(() -> Isbn.de(null))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Informe o ISBN.");
    }

    @Test
    void construtorDiretoNaoAceitaValorInvalido() {
        assertThatThrownBy(() -> new Isbn("9788545702871")).isInstanceOf(IllegalArgumentException.class);
    }
}
