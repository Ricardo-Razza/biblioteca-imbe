package br.gov.imbe.biblioteca.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.comum.erro.IndisponivelException;

class ValidadorAdTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private ValidadorAd validador(String url) {
        AuthProperties p = new AuthProperties(new AuthProperties.Jwt("x".repeat(32), Duration.ofHours(8)),
                new AuthProperties.Ad(url == null ? null : URI.create(url), "token-secreto", Duration.ofMillis(500)),
                5, Duration.ofMinutes(15));
        return new ValidadorAd(RestClient.builder(), p);
    }

    @Test
    void mandaUsuarioESenhaComOBearerDoServidor() {
        api.stubFor(post("/login").willReturn(ok()));
        assertThat(validador(api.baseUrl() + "/login").validar("maria", "s3nha")).isTrue();
        api.verify(postRequestedFor(urlEqualTo("/login"))
                .withHeader("Authorization", equalTo("Bearer token-secreto"))
                .withRequestBody(equalToJson("{\"usuario\": \"maria\", \"senha\": \"s3nha\"}")));
    }

    @Test
    void credencialRecusada() {
        api.stubFor(post("/login").willReturn(aResponse().withStatus(401)));
        assertThat(validador(api.baseUrl() + "/login").validar("maria", "errada")).isFalse();
    }

    @Test
    void foraDoArViraIndisponivel() {
        api.stubFor(post("/login").willReturn(aResponse().withStatus(502)));
        assertThatThrownBy(() -> validador(api.baseUrl() + "/login").validar("maria", "x"))
                .isInstanceOf(IndisponivelException.class);

        api.stubFor(post("/login").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> validador(api.baseUrl() + "/login").validar("maria", "x"))
                .isInstanceOf(IndisponivelException.class);

        api.stubFor(post("/login").willReturn(ok().withFixedDelay(2000)));
        assertThatThrownBy(() -> validador(api.baseUrl() + "/login").validar("maria", "x"))
                .isInstanceOf(IndisponivelException.class);
    }

    @Test
    void semUrlConfiguradaAvisa() {
        assertThatThrownBy(() -> validador(null).validar("maria", "x"))
                .isInstanceOf(IndisponivelException.class)
                .hasMessageContaining("não está configurado");
    }
}
