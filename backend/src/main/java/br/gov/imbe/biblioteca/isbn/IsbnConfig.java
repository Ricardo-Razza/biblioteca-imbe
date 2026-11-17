package br.gov.imbe.biblioteca.isbn;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IsbnProperties.class)
public class IsbnConfig {

    static final String USER_AGENT = "BibliotecaMunicipalImbe/1.0";

    /**
     * RestClient de uma fonte: endereço base, timeout de conexão e de leitura e User-Agent próprio.
     * Estático para os testes montarem o provider sem subir o Spring.
     */
    public static RestClient clientePara(RestClient.Builder builder, String urlBase, Duration timeout) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(timeout);
        return builder.clone()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }
}
