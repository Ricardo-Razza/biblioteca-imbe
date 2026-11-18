package br.gov.imbe.biblioteca.isbn.fonte;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.FonteIsbnIndisponivelException;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.PropriedadesDeTeste;

class OpenLibraryProviderTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Isbn ISBN = Isbn.de("9780306406157");

    OpenLibraryProvider provider;

    @BeforeEach
    void montar() {
        provider = new OpenLibraryProvider(RestClient.builder(), PropriedadesDeTeste.apontandoPara(api.baseUrl()));
    }

    private void edicao(String json) {
        // A Open Library responde /isbn/{isbn}.json com redirecionamento para a edição
        api.stubFor(get("/isbn/9780306406157.json")
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/books/OL1M.json")));
        api.stubFor(get("/books/OL1M.json").willReturn(okJson(json)));
    }

    @Test
    void segueORedirecionamentoEBuscaOsAutores() {
        edicao("""
                {
                  "title": "Probability theory",
                  "subtitle": "the logic of science",
                  "publishers": ["Cambridge University Press", "Outra"],
                  "publish_date": "June 9, 2003",
                  "number_of_pages": 727,
                  "authors": [{"key": "/authors/OL1A"}, {"key": "/authors/OL2A"}],
                  "languages": [{"key": "/languages/eng"}],
                  "subjects": ["Probabilities"],
                  "description": {"type": "/type/text", "value": "Um clássico."},
                  "covers": [12345]
                }
                """);
        api.stubFor(get("/authors/OL1A.json").willReturn(okJson("{\"name\": \"E. T. Jaynes\"}")));
        api.stubFor(get("/authors/OL2A.json").willReturn(okJson("{\"name\": \"G. Larry Bretthorst\"}")));

        DadosLivro dados = provider.buscar(ISBN).orElseThrow();

        assertThat(dados.titulo()).isEqualTo("Probability theory");
        assertThat(dados.subtitulo()).isEqualTo("the logic of science");
        assertThat(dados.autores()).containsExactly("E. T. Jaynes", "G. Larry Bretthorst");
        assertThat(dados.editora()).isEqualTo("Cambridge University Press");
        assertThat(dados.ano()).isEqualTo(2003);
        assertThat(dados.paginas()).isEqualTo(727);
        assertThat(dados.idioma()).isEqualTo("en");
        assertThat(dados.sinopse()).isEqualTo("Um clássico.");
        assertThat(dados.assuntos()).containsExactly("Probabilities");
        assertThat(dados.capas()).containsExactly(api.baseUrl() + "/b/isbn/9780306406157-L.jpg?default=false");
        assertThat(dados.fonte()).isEqualTo(FonteIsbn.OPEN_LIBRARY);
    }

    @Test
    void descricaoEmTextoSimplesETentaACapaPeloIsbn() {
        edicao("""
                {"title": "Livro", "description": "Texto simples.", "languages": [{"key": "/languages/por"}],
                 "pagination": "xii, 417 p. :"}
                """);

        DadosLivro dados = provider.buscar(ISBN).orElseThrow();

        assertThat(dados.paginas()).isEqualTo(417);
        assertThat(dados.sinopse()).isEqualTo("Texto simples.");
        assertThat(dados.idioma()).isEqualTo("pt");
        // Sem "covers" na edição a capa ainda pode existir pelo ISBN; o download confere
        assertThat(dados.capas()).containsExactly(api.baseUrl() + "/b/isbn/9780306406157-L.jpg?default=false");
        assertThat(dados.autores()).isEmpty();
    }

    @Test
    void autorQueFalhaFicaDeForaSemDerrubarAEdicao() {
        edicao("""
                {"title": "Livro", "authors": [{"key": "/authors/OL1A"}, {"key": "/authors/OL2A"}]}
                """);
        api.stubFor(get("/authors/OL1A.json").willReturn(serverError()));
        api.stubFor(get("/authors/OL2A.json").willReturn(okJson("{\"name\": \"Autora Dois\"}")));

        assertThat(provider.buscar(ISBN).orElseThrow().autores()).containsExactly("Autora Dois");
    }

    @Test
    void naoEncontradoDevolveVazio() {
        api.stubFor(get("/isbn/9780306406157.json").willReturn(notFound()));

        assertThat(provider.buscar(ISBN)).isEmpty();
    }

    @Test
    void erroDoServidorViraFonteIndisponivel() {
        api.stubFor(get("/isbn/9780306406157.json").willReturn(serverError()));

        assertThatThrownBy(() -> provider.buscar(ISBN))
                .isInstanceOf(FonteIsbnIndisponivelException.class)
                .hasMessageContaining("HTTP 500");
    }
}
