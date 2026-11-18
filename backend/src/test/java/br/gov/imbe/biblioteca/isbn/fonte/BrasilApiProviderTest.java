package br.gov.imbe.biblioteca.isbn.fonte;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.FonteIsbnIndisponivelException;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.PropriedadesDeTeste;

class BrasilApiProviderTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Isbn AKIRA = Isbn.de("9788545702870");
    private static final String CAMINHO = "/api/isbn/v1/9788545702870";
    private static final Duration TIMEOUT = PropriedadesDeTeste.TIMEOUT;

    BrasilApiProvider provider;

    @BeforeEach
    void montar() {
        provider = new BrasilApiProvider(RestClient.builder(), PropriedadesDeTeste.apontandoPara(api.baseUrl()));
    }

    @Test
    void converteARespostaENormalizaOsCampos() {
        api.stubFor(get(CAMINHO).willReturn(okJson("""
                {
                  "isbn": "9788545702870",
                  "title": "  Akira  ",
                  "subtitle": "",
                  "authors": ["KATSUHIRO OTOMO", " ", "KATSUHIRO OTOMO"],
                  "publisher": "JBC",
                  "synopsis": "Tóquio, 2019.",
                  "dimensions": {"width": 17.5, "height": 25.7, "unit": "CENTIMETER"},
                  "year": 2017,
                  "format": "PHYSICAL",
                  "page_count": 364,
                  "subjects": ["Mangá", "Ficção científica"],
                  "location": "SÃO PAULO, SP",
                  "retail_price": null,
                  "cover_url": null,
                  "provider": "cbl"
                }
                """)));

        DadosLivro dados = provider.buscar(AKIRA).orElseThrow();

        assertThat(dados.isbn13()).isEqualTo("9788545702870");
        assertThat(dados.titulo()).isEqualTo("Akira");
        assertThat(dados.subtitulo()).isNull();
        assertThat(dados.autores()).containsExactly("KATSUHIRO OTOMO");
        assertThat(dados.editora()).isEqualTo("JBC");
        assertThat(dados.ano()).isEqualTo(2017);
        assertThat(dados.paginas()).isEqualTo(364);
        assertThat(dados.idioma()).isNull();
        assertThat(dados.sinopse()).isEqualTo("Tóquio, 2019.");
        assertThat(dados.assuntos()).containsExactly("Mangá", "Ficção científica");
        assertThat(dados.capas()).isEmpty();
        assertThat(dados.fonte()).isEqualTo(FonteIsbn.BRASILAPI);

        api.verify(getRequestedFor(urlEqualTo(CAMINHO)).withHeader("User-Agent", matching("BibliotecaMunicipalImbe/.*")));
    }

    @Test
    void naoEncontradoDevolveVazio() {
        api.stubFor(get(CAMINHO).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"ISBN não encontrado\",\"type\":\"not_found\"}")));

        assertThat(provider.buscar(AKIRA)).isEmpty();
    }

    @Test
    void respostaSemTituloContaComoNaoEncontrado() {
        api.stubFor(get(CAMINHO).willReturn(okJson("{\"isbn\":\"9788545702870\",\"title\":\" \"}")));

        assertThat(provider.buscar(AKIRA)).isEmpty();
    }

    @Test
    void erroDoServidorViraFonteIndisponivel() {
        api.stubFor(get(CAMINHO).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> provider.buscar(AKIRA))
                .isInstanceOf(FonteIsbnIndisponivelException.class)
                .hasMessageContaining("HTTP 503");
    }

    @Test
    void demoraAlemDoTimeoutViraFonteIndisponivel() {
        api.stubFor(get(CAMINHO).willReturn(okJson("{\"title\":\"Akira\"}").withFixedDelay((int) TIMEOUT.toMillis() + 1000)));

        assertThatThrownBy(() -> provider.buscar(AKIRA))
                .isInstanceOf(FonteIsbnIndisponivelException.class)
                .satisfies(e -> assertThat(((FonteIsbnIndisponivelException) e).getFonte()).isEqualTo(FonteIsbn.BRASILAPI));
    }

    @Test
    void conexaoDerrubadaViraFonteIndisponivel() {
        api.stubFor(get(CAMINHO).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> provider.buscar(AKIRA)).isInstanceOf(FonteIsbnIndisponivelException.class);
    }

    @Test
    void jsonIlegivelViraFonteIndisponivel() {
        api.stubFor(get(CAMINHO).willReturn(okJson("<html>manutenção</html>")));

        assertThatThrownBy(() -> provider.buscar(AKIRA)).isInstanceOf(FonteIsbnIndisponivelException.class);
    }
}
