package br.gov.imbe.biblioteca.acervo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

class BaixadorDeCapaTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static BaixadorDeCapa baixador(boolean permitirLocais, DataSize limite) {
        return new BaixadorDeCapa(new CapaProperties(Path.of("target"), 800, 200, limite, Duration.ofSeconds(2),
                permitirLocais));
    }

    private static final BaixadorDeCapa LOCAL = baixador(true, DataSize.ofMegabytes(5));
    private static final byte[] JPEG = Imagens.jpeg(50, 80);

    @Test
    void baixaImagem() {
        api.stubFor(get("/capa.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg").withBody(JPEG)));

        assertThat(LOCAL.baixar(api.baseUrl() + "/capa.jpg")).hasValue(JPEG);
    }

    @Test
    void segueRedirecionamento() {
        api.stubFor(get("/b/isbn/1-L.jpg").willReturn(aResponse().withStatus(302).withHeader("Location", "/arquivo/1.jpg")));
        api.stubFor(get("/arquivo/1.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg").withBody(JPEG)));

        assertThat(LOCAL.baixar(api.baseUrl() + "/b/isbn/1-L.jpg")).hasValue(JPEG);
    }

    @Test
    void desisteDeRedirecionamentoEmCirculo() {
        api.stubFor(get("/a").willReturn(aResponse().withStatus(302).withHeader("Location", "/b")));
        api.stubFor(get("/b").willReturn(aResponse().withStatus(302).withHeader("Location", "/a")));

        assertThat(LOCAL.baixar(api.baseUrl() + "/a")).isEmpty();
    }

    @Test
    void semCapaDevolveVazio() {
        api.stubFor(get("/nada.jpg").willReturn(notFound()));

        assertThat(LOCAL.baixar(api.baseUrl() + "/nada.jpg")).isEmpty();
    }

    @Test
    void recusaQuemNaoEImagem() {
        api.stubFor(get("/pagina").willReturn(aResponse().withHeader("Content-Type", "text/html").withBody("<html>")));

        assertThat(LOCAL.baixar(api.baseUrl() + "/pagina")).isEmpty();
    }

    @Test
    void recusaArquivoAcimaDoLimite() {
        api.stubFor(get("/enorme.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg")
                .withBody(new byte[2048])));

        assertThat(baixador(true, DataSize.ofBytes(1024)).baixar(api.baseUrl() + "/enorme.jpg")).isEmpty();
    }

    @Test
    void emProducaoRecusaEnderecoInterno() {
        api.stubFor(get("/capa.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg").withBody(JPEG)));

        assertThat(baixador(false, DataSize.ofMegabytes(5)).baixar(api.baseUrl() + "/capa.jpg")).isEmpty();
        api.verify(0, com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/capa.jpg")));
    }

    @Test
    void recusaEsquemaQueNaoEHttp() {
        assertThat(LOCAL.baixar("file:///etc/passwd")).isEmpty();
        assertThat(LOCAL.baixar("ftp://exemplo.org/capa.jpg")).isEmpty();
        assertThat(LOCAL.baixar("não é url")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.0.10", "169.254.169.254", "100.64.0.1",
            "0.0.0.0", "::1", "fd00::1", "fe80::1" })
    void enderecosInternos(String ip) throws Exception {
        assertThat(BaixadorDeCapa.interno(InetAddress.getByName(ip))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "8.8.8.8", "200.160.2.3", "2001:4860:4860::8888" })
    void enderecosPublicos(String ip) throws Exception {
        assertThat(BaixadorDeCapa.interno(InetAddress.getByName(ip))).isFalse();
    }
}
