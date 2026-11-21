package br.gov.imbe.biblioteca.publico;

import static br.gov.imbe.biblioteca.publico.RegistroDeBuscas.normalizar;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistroDeBuscasTest {

    @Test
    void guardaOTermoLimpoEmMinusculas() {
        assertThat(normalizar("  O   Cortiço ")).isEqualTo("o cortiço");
        assertThat(normalizar("1984")).isEqualTo("1984");
        assertThat(normalizar("x".repeat(150))).hasSize(RegistroDeBuscas.TAMANHO_MAXIMO);
    }

    @Test
    void descartaTermoCurtoOuComCaraDeDadoPessoal() {
        assertThat(normalizar(null)).isNull();
        assertThat(normalizar(" ab ")).isNull();
        assertThat(normalizar("123.456.789-09")).isNull();
        assertThat(normalizar("(51) 99812-4410")).isNull();
        assertThat(normalizar("joana 12345")).isNull();
    }
}
