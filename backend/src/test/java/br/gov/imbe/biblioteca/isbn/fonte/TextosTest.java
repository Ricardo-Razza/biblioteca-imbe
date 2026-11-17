package br.gov.imbe.biblioteca.isbn.fonte;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextosTest {

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
            "2017-05-01,   2017",
            "'May 1, 2017', 2017",
            "c1999,        1999",
            "2002,         2002",
            "s.d.,         NULO",
            "NULO,         NULO"
    })
    void ano(String data, Integer esperado) {
        assertThat(Textos.ano(data)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
            "pt-BR, pt",
            "pt,    pt",
            "por,   pt",
            "eng,   en",
            "xyz,   xyz",
            "'  ',  NULO"
    })
    void idioma(String codigo, String esperado) {
        assertThat(Textos.idioma(codigo)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
            "'417 p. :',      417",
            "'xii, 300 p.',   300",
            "'300p',          300",
            "'1 v. (várias paginações)', NULO",
            "NULO,            NULO"
    })
    void paginas(String paginacao, Integer esperado) {
        assertThat(Textos.paginas(paginacao)).isEqualTo(esperado);
    }
}
