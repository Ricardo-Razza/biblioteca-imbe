package br.gov.imbe.biblioteca.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.gov.imbe.biblioteca.acervo.ObraRequisicao;
import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;

class ObraDaFonteTest {

    @Test
    void cortaOQuePassaDosLimitesDoCadastro() {
        List<String> autores = IntStream.range(0, 25).mapToObj(i -> "Autor " + i).toList();
        DadosLivro d = new DadosLivro("9788535914849", "T".repeat(600), null, autores, "Editora", 1200, 120000, "pt-BR",
                null, List.of("Romance"), List.of(), FonteIsbn.BRASILAPI);
        ObraRequisicao r = ObraDaFonte.de(d);
        assertThat(r.titulo()).hasSize(500);
        assertThat(r.autores()).hasSize(20);
        assertThat(r.ano()).isNull();
        assertThat(r.paginas()).isNull();
        assertThat(r.idioma()).isEqualTo("pt-BR");
        assertThat(r.fonteDados()).isEqualTo(FonteIsbn.BRASILAPI);
        assertThat(r.assuntos()).containsExactly("Romance");
    }

    @Test
    void leituraDaPlanilha() {
        List<LinhaPlanilha> l = LeitorDePlanilha.ler(("﻿isbn,tombo\n# comentário\n85-359-0277-5, a.1 ,\"Sala 2; fundo\"\n"
                + "9788535914849\tB-2\n12345\n9788535914849;tombo com espaço\n").getBytes(StandardCharsets.UTF_8));
        assertThat(l).hasSize(4);
        assertThat(l.get(0)).isEqualTo(new LinhaPlanilha(3, "85-359-0277-5", "9788535902778", "A.1", "Sala 2; fundo", null));
        assertThat(l.get(1).tombo()).isEqualTo("B-2");
        assertThat(l.get(1).numero()).isEqualTo(4);
        assertThat(l.get(2).erro()).isEqualTo("O ISBN deve ter 10 ou 13 dígitos.");
        assertThat(l.get(3).erro()).startsWith("Tombo inválido (tombo com espaço)");
    }

    @Test
    void limiteDeLinhas() {
        String muitas = String.join("\n", Collections.nCopies(LeitorDePlanilha.MAXIMO_LINHAS + 1, "9788535914849"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> LeitorDePlanilha.ler(muitas.getBytes()))
                .hasMessageContaining("no máximo 5000 linhas");
    }
}
