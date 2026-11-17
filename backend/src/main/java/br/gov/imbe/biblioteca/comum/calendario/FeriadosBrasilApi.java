package br.gov.imbe.biblioteca.comum.calendario;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.gov.imbe.biblioteca.comum.erro.IndisponivelException;
import br.gov.imbe.biblioteca.isbn.IsbnConfig;

/** Feriados nacionais do ano: {@code GET /api/feriados/v1/{ano}} da BrasilAPI (inclui os móveis, como o Carnaval). */
@Component
public class FeriadosBrasilApi {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeriadoExterno(LocalDate date, String name, String type) {
    }

    private final RestClient cliente;

    public FeriadosBrasilApi(RestClient.Builder builder,
            @Value("${biblioteca.feriados.url:https://brasilapi.com.br}") String url,
            @Value("${biblioteca.feriados.timeout:4s}") Duration timeout) {
        this.cliente = IsbnConfig.clientePara(builder, url, timeout);
    }

    public List<FeriadoExterno> doAno(int ano) {
        try {
            List<FeriadoExterno> lista = cliente.get().uri("/api/feriados/v1/{ano}", ano).retrieve()
                    .body(new ParameterizedTypeReference<List<FeriadoExterno>>() {
                    });
            return lista == null ? List.of()
                    : lista.stream().filter(f -> f.date() != null && f.name() != null && !f.name().isBlank()).toList();
        } catch (RestClientException e) {
            throw new IndisponivelException("Não foi possível buscar os feriados nacionais agora. Tente mais tarde.");
        }
    }
}
