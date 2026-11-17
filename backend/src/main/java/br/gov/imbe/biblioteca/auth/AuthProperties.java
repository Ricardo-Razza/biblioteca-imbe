package br.gov.imbe.biblioteca.auth;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Login e JWT ({@code biblioteca.auth.*} no application.yml). */
@ConfigurationProperties("biblioteca.auth")
public record AuthProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Ad ad,
        @DefaultValue("5") int tentativas,
        @DefaultValue("15m") Duration bloqueio) {

    /** @param segredo {@code JWT_SECRET}, pelo menos 32 caracteres */
    public record Jwt(String segredo, @DefaultValue("8h") Duration validade) {
    }

    /** API de login da Prefeitura. Sem URL, só os usuários LOCAL entram. */
    public record Ad(URI url, String token, @DefaultValue("4s") Duration timeout) {

        public boolean configurado() {
            return url != null && !url.toString().isBlank();
        }
    }
}
