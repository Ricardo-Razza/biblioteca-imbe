package br.gov.imbe.biblioteca.isbn;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração das fontes de ISBN ({@code biblioteca.isbn.*} no application.yml). */
@ConfigurationProperties("biblioteca.isbn")
public record IsbnProperties(
        @DefaultValue("4s") Duration timeout,
        @DefaultValue("24h") Duration cacheTtl,
        @DefaultValue BrasilApi brasilapi,
        @DefaultValue GoogleBooks googleBooks,
        @DefaultValue OpenLibrary openLibrary) {

    public record BrasilApi(@DefaultValue("https://brasilapi.com.br") URI url) {
    }

    /** @param chave {@code GOOGLE_BOOKS_API_KEY}; vazia funciona com cota menor */
    public record GoogleBooks(@DefaultValue("https://www.googleapis.com") URI url, String chave) {

        public boolean temChave() {
            return chave != null && !chave.isBlank();
        }
    }

    public record OpenLibrary(
            @DefaultValue("https://openlibrary.org") URI url,
            @DefaultValue("https://covers.openlibrary.org") URI capasUrl) {
    }
}
