package br.gov.imbe.biblioteca.isbn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class IsbnLookupServiceTest {

    private static final Isbn ISBN = Isbn.de("9788545702870");

    /** Fonte falsa: conta as chamadas e devolve o que o teste mandar (ou lança). */
    static final class FonteFalsa implements IsbnProvider {
        final FonteIsbn fonte;
        final Supplier<Optional<DadosLivro>> resposta;
        final AtomicInteger chamadas = new AtomicInteger();

        FonteFalsa(FonteIsbn fonte, Supplier<Optional<DadosLivro>> resposta) {
            this.fonte = fonte;
            this.resposta = resposta;
        }

        @Override
        public FonteIsbn fonte() {
            return fonte;
        }

        @Override
        public Optional<DadosLivro> buscar(Isbn isbn) {
            chamadas.incrementAndGet();
            return resposta.get();
        }
    }

    private static DadosLivro dados(FonteIsbn fonte, String titulo, String editora, Integer paginas,
            List<String> autores, String capa) {
        return new DadosLivro(ISBN.valor(), titulo, null, autores, editora, null, paginas, null, null, null, capa,
                fonte);
    }

    private static IsbnLookupService servico(IsbnProvider... fontes) {
        return new IsbnLookupService(List.of(fontes), PropriedadesDeTeste.apontandoPara("http://nao-usado"));
    }

    @Test
    void aPrimeiraFonteDefineEAsSeguintesCompletam() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI,
                () -> Optional.of(dados(FonteIsbn.BRASILAPI, "Akira", null, 364, List.of(), null)));
        var google = new FonteFalsa(FonteIsbn.GOOGLE_BOOKS,
                () -> Optional.of(dados(FonteIsbn.GOOGLE_BOOKS, "Akira Vol. 1", "JBC", 999,
                        List.of("Katsuhiro Otomo"), "https://google/capa")));
        var open = new FonteFalsa(FonteIsbn.OPEN_LIBRARY,
                () -> Optional.of(dados(FonteIsbn.OPEN_LIBRARY, "Outro", "Outra", 1, List.of("X"),
                        "https://openlibrary/capa")));

        ResultadoIsbn r = servico(brasil, google, open).buscar(ISBN);

        DadosLivro d = r.dados().orElseThrow();
        assertThat(d.titulo()).isEqualTo("Akira");
        assertThat(d.paginas()).isEqualTo(364);
        assertThat(d.editora()).isEqualTo("JBC");
        assertThat(d.autores()).containsExactly("Katsuhiro Otomo");
        assertThat(d.capaUrl()).isEqualTo("https://google/capa");
        assertThat(d.fonte()).isEqualTo(FonteIsbn.BRASILAPI);
        assertThat(r.fontesComDados()).containsExactly(FonteIsbn.BRASILAPI, FonteIsbn.GOOGLE_BOOKS,
                FonteIsbn.OPEN_LIBRARY);
        assertThat(r.fontesIndisponiveis()).isEmpty();
    }

    @Test
    void semAPrimeiraFonteASegundaDefine() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI, Optional::empty);
        var google = new FonteFalsa(FonteIsbn.GOOGLE_BOOKS,
                () -> Optional.of(dados(FonteIsbn.GOOGLE_BOOKS, "Akira", "JBC", null, List.of(), null)));

        DadosLivro d = servico(brasil, google).buscar(ISBN).dados().orElseThrow();

        assertThat(d.titulo()).isEqualTo("Akira");
        assertThat(d.fonte()).isEqualTo(FonteIsbn.GOOGLE_BOOKS);
    }

    @Test
    void naoEncontradoVaiParaOCache() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI, Optional::empty);
        var google = new FonteFalsa(FonteIsbn.GOOGLE_BOOKS, Optional::empty);
        IsbnLookupService servico = servico(brasil, google);

        ResultadoIsbn primeira = servico.buscar(ISBN);
        ResultadoIsbn segunda = servico.buscar(ISBN);

        assertThat(primeira.encontrado()).isFalse();
        assertThat(segunda).isSameAs(primeira);
        assertThat(brasil.chamadas).hasValue(1);
        assertThat(google.chamadas).hasValue(1);
    }

    @Test
    void encontradoVaiParaOCache() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI,
                () -> Optional.of(dados(FonteIsbn.BRASILAPI, "Akira", null, null, List.of(), null)));
        IsbnLookupService servico = servico(brasil);

        servico.buscar(ISBN);
        servico.buscar(Isbn.de("978-85-457-0287-0"));

        assertThat(brasil.chamadas).hasValue(1);
    }

    @Test
    void fonteIndisponivelNaoVaiParaOCacheMasAsOutrasValem() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI, () -> {
            throw new FonteIsbnIndisponivelException(FonteIsbn.BRASILAPI, "HTTP 503", null);
        });
        var google = new FonteFalsa(FonteIsbn.GOOGLE_BOOKS,
                () -> Optional.of(dados(FonteIsbn.GOOGLE_BOOKS, "Akira", null, null, List.of(), null)));
        IsbnLookupService servico = servico(brasil, google);

        ResultadoIsbn r = servico.buscar(ISBN);
        servico.buscar(ISBN);

        assertThat(r.dados().orElseThrow().titulo()).isEqualTo("Akira");
        assertThat(r.fontesIndisponiveis()).containsExactly(FonteIsbn.BRASILAPI);
        assertThat(r.completo()).isFalse();
        assertThat(brasil.chamadas).hasValue(2);
        assertThat(google.chamadas).hasValue(2);
    }

    @Test
    void erroInesperadoNumaFonteContaComoIndisponivel() {
        var brasil = new FonteFalsa(FonteIsbn.BRASILAPI, () -> {
            throw new IllegalStateException("bug");
        });

        ResultadoIsbn r = servico(brasil).buscar(ISBN);

        assertThat(r.encontrado()).isFalse();
        assertThat(r.fontesIndisponiveis()).containsExactly(FonteIsbn.BRASILAPI);
    }

    @Test
    void consultaAsFontesEmParalelo() {
        Supplier<Optional<DadosLivro>> lenta = () -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        };
        IsbnLookupService servico = servico(new FonteFalsa(FonteIsbn.BRASILAPI, lenta),
                new FonteFalsa(FonteIsbn.GOOGLE_BOOKS, lenta), new FonteFalsa(FonteIsbn.OPEN_LIBRARY, lenta));

        long inicio = System.nanoTime();
        servico.buscar(ISBN);
        Duration gasto = Duration.ofNanos(System.nanoTime() - inicio);

        // Em série seriam 2,1 s
        assertThat(gasto).isLessThan(Duration.ofMillis(1800));
    }
}
