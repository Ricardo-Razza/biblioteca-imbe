package br.gov.imbe.biblioteca.comum.calendario;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.comum.erro.IndisponivelException;

class FeriadosBrasilApiTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private FeriadosBrasilApi cliente() {
        return new FeriadosBrasilApi(RestClient.builder(), api.baseUrl(), Duration.ofSeconds(2));
    }

    @Test
    void leOsFeriadosDoAnoEDescartaItensIncompletos() {
        api.stubFor(get("/api/feriados/v1/2027").willReturn(okJson("""
                [
                  {"date": "2027-01-01", "name": "Confraternização mundial", "type": "national"},
                  {"date": "2027-02-09", "name": "Carnaval", "type": "national", "extra": 1},
                  {"date": null, "name": "Sem data"},
                  {"date": "2027-04-21", "name": " "}
                ]
                """)));
        assertThat(cliente().doAno(2027))
                .extracting(FeriadosBrasilApi.FeriadoExterno::date, FeriadosBrasilApi.FeriadoExterno::name)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2027, 1, 1), "Confraternização mundial"),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2027, 2, 9), "Carnaval"));
    }

    @Test
    void foraDoArViraIndisponivel() {
        api.stubFor(get("/api/feriados/v1/2027").willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> cliente().doAno(2027)).isInstanceOf(IndisponivelException.class);

        api.stubFor(get("/api/feriados/v1/2028").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> cliente().doAno(2028)).isInstanceOf(IndisponivelException.class);
    }
}
