package br.gov.imbe.biblioteca.isbn;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração das fontes de ISBN ({@code biblioteca.isbn.*} no application.yml). */
@ConfigurationProperties("biblioteca.isbn")
public record IsbnProperties(
        @DefaultValue("4s") Duration timeout,
        @DefaultValue Fonte brasilapi) {

    public record Fonte(@DefaultValue("https://brasilapi.com.br") URI url) {
    }
}
