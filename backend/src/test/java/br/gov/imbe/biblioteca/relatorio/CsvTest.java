package br.gov.imbe.biblioteca.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void celulas() {
        assertThat(Csv.celula(null)).isEmpty();
        assertThat(Csv.celula(LocalDate.of(2026, 11, 7))).isEqualTo("07/11/2026");
        assertThat(Csv.celula(LocalDateTime.of(2026, 11, 7, 9, 5))).isEqualTo("07/11/2026 09:05");
        assertThat(Csv.celula(-3)).isEqualTo("-3");
        assertThat(Csv.celula("Dom Casmurro")).isEqualTo("Dom Casmurro");
        assertThat(Csv.celula("Contos; crônicas")).isEqualTo("\"Contos; crônicas\"");
        assertThat(Csv.celula("O \"grande\" livro")).isEqualTo("\"O \"\"grande\"\" livro\"");
        assertThat(Csv.celula("linha\nquebrada")).isEqualTo("\"linha\nquebrada\"");
    }

    @Test
    void formulasViramTexto() {
        assertThat(Csv.celula("=1+1")).isEqualTo("'=1+1");
        assertThat(Csv.celula("+5")).isEqualTo("'+5");
        assertThat(Csv.celula("-x")).isEqualTo("'-x");
        assertThat(Csv.celula("@SOMA(A1)")).isEqualTo("'@SOMA(A1)");
        assertThat(Csv.celula("\tx")).isEqualTo("'\tx");
    }

    @Test
    void arquivoComBomEcrlf() {
        byte[] b = new Csv().linha("a", 1).linha("b", null).bytes();
        assertThat(new String(b, StandardCharsets.UTF_8)).isEqualTo("﻿a;1\r\nb;\r\n");
    }
}
