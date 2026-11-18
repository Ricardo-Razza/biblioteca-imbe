package br.gov.imbe.biblioteca.isbn.fonte;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.FonteIsbnIndisponivelException;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.PropriedadesDeTeste;

class GoogleBooksProviderTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Isbn ISBN = Isbn.de("9788535902778");
    private static final String CAMINHO = "/books/v1/volumes";

    private GoogleBooksProvider provider(String chave) {
        return new GoogleBooksProvider(RestClient.builder(), PropriedadesDeTeste.apontandoPara(api.baseUrl(), chave));
    }

    @Test
    void converteOPrimeiroVolume() {
        api.stubFor(get(urlPathEqualTo(CAMINHO)).withQueryParam("q", equalTo("isbn:9788535902778"))
                .willReturn(okJson("""
                        {
                          "kind": "books#volumes",
                          "totalItems": 1,
                          "items": [{
                            "volumeInfo": {
                              "title": "Ensaio sobre a cegueira",
                              "authors": ["José Saramago"],
                              "publisher": "Companhia das Letras",
                              "publishedDate": "1995-10-01",
                              "description": "Uma epidemia de cegueira branca.",
                              "pageCount": 312,
                              "categories": ["Fiction"],
                              "language": "pt-BR",
                              "imageLinks": {
                                "smallThumbnail": "http://books.google.com/books/content?id=x&zoom=5",
                                "thumbnail": "http://books.google.com/books/content?id=x&zoom=1"
                              }
                            }
                          }]
                        }
                        """)));

        DadosLivro dados = provider(null).buscar(ISBN).orElseThrow();

        assertThat(dados.titulo()).isEqualTo("Ensaio sobre a cegueira");
        assertThat(dados.autores()).containsExactly("José Saramago");
        assertThat(dados.editora()).isEqualTo("Companhia das Letras");
        assertThat(dados.ano()).isEqualTo(1995);
        assertThat(dados.paginas()).isEqualTo(312);
        assertThat(dados.idioma()).isEqualTo("pt");
        assertThat(dados.sinopse()).isEqualTo("Uma epidemia de cegueira branca.");
        assertThat(dados.assuntos()).containsExactly("Fiction");
        assertThat(dados.capas()).containsExactly("https://books.google.com/books/content?id=x&zoom=1&fife=w800",
                "https://books.google.com/books/content?id=x&zoom=1");
        assertThat(dados.fonte()).isEqualTo(FonteIsbn.GOOGLE_BOOKS);
    }

    @Test
    void semItensDevolveVazio() {
        api.stubFor(get(urlPathEqualTo(CAMINHO)).willReturn(okJson("{\"kind\":\"books#volumes\",\"totalItems\":0}")));

        assertThat(provider(null).buscar(ISBN)).isEmpty();
    }

    @Test
    void enviaAChaveSoQuandoConfigurada() {
        api.stubFor(get(urlPathEqualTo(CAMINHO)).willReturn(okJson("{\"totalItems\":0}")));

        provider("chave-secreta").buscar(ISBN);
        provider(" ").buscar(ISBN);

        api.verify(1, getRequestedFor(urlPathEqualTo(CAMINHO)).withQueryParam("key", equalTo("chave-secreta")));
        api.verify(1, getRequestedFor(urlPathEqualTo(CAMINHO)).withQueryParam("key", absent()));
    }

    @Test
    void falhaNaoExpoeAChave() {
        api.stubFor(get(urlPathEqualTo(CAMINHO))
                .willReturn(okJson("{}").withFixedDelay((int) PropriedadesDeTeste.TIMEOUT.toMillis() + 1000)));

        assertThatThrownBy(() -> provider("chave-secreta").buscar(ISBN))
                .isInstanceOf(FonteIsbnIndisponivelException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("chave-secreta");
                    for (Throwable c = e.getCause(); c != null; c = c.getCause()) {
                        assertThat(String.valueOf(c.getMessage())).doesNotContain("chave-secreta");
                    }
                });
    }

    @Test
    void cotaEstouradaViraFonteIndisponivel() {
        api.stubFor(get(urlPathEqualTo(CAMINHO)).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> provider(null).buscar(ISBN))
                .isInstanceOf(FonteIsbnIndisponivelException.class)
                .hasMessageContaining("HTTP 429");
    }
}
