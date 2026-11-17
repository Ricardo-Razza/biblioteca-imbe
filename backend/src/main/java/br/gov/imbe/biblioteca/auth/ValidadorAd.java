package br.gov.imbe.biblioteca.auth;

import java.net.http.HttpClient;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.gov.imbe.biblioteca.comum.erro.IndisponivelException;

/**
 * Valida login e senha na API de login da Prefeitura (AD). Não é LDAP: só consome a API existente.
 * <p>
 * Contrato provisório, até a Prefeitura documentar o real ({@code docs/referencia/api-auth-ad.md}):
 * {@code POST AD_AUTH_URL} com {@code Authorization: Bearer AD_AUTH_TOKEN} e corpo
 * {@code {"usuario": "...", "senha": "..."}}. 2xx aceita; 400, 401, 403 e 404 recusam; o resto, ou sem resposta
 * no timeout, é indisponibilidade.
 */
@Component
public class ValidadorAd {

    private static final Logger log = LoggerFactory.getLogger(ValidadorAd.class);

    private final AuthProperties.Ad config;
    private final RestClient cliente;

    public ValidadorAd(RestClient.Builder builder, AuthProperties propriedades) {
        this.config = propriedades.ad();
        // HTTP/1.1: o upgrade para HTTP/2 sem TLS (h2c) em POST com corpo falha em vários servidores
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(config.timeout()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(config.timeout());
        this.cliente = builder.clone().requestFactory(fabrica).build();
    }

    /** @return se a senha confere; lança {@link IndisponivelException} se a API não respondeu direito */
    public boolean validar(String login, String senha) {
        if (!config.configurado()) {
            throw new IndisponivelException("O login pela rede da Prefeitura não está configurado. "
                    + "Peça a um administrador com usuário local.");
        }
        try {
            return cliente.post()
                    .uri(config.url())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + (config.token() == null ? "" : config.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("usuario", login, "senha", senha))
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (resposta.getStatusCode().is2xxSuccessful()) {
                            return true;
                        }
                        if (status == 400 || status == 401 || status == 403 || status == 404) {
                            return false;
                        }
                        log.warn("API de login AD respondeu HTTP {}", status);
                        throw indisponivel();
                    });
        } catch (RestClientException e) {
            log.warn("API de login AD sem resposta: {}", e.getMessage());
            throw indisponivel();
        }
    }

    private static IndisponivelException indisponivel() {
        return new IndisponivelException("A validação pela rede da Prefeitura não respondeu. "
                + "Tente de novo em alguns minutos.");
    }
}
