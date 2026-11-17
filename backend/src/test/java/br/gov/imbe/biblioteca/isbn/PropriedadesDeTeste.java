package br.gov.imbe.biblioteca.isbn;

import java.net.URI;
import java.time.Duration;

/** Todas as fontes apontando para o mesmo servidor (WireMock). */
public final class PropriedadesDeTeste {

    /** Folga para o primeiro request da JVM fria; testes de timeout usam um atraso maior que isso. */
    public static final Duration TIMEOUT = Duration.ofSeconds(2);

    private PropriedadesDeTeste() {
    }

    public static IsbnProperties apontandoPara(String urlBase, String chaveGoogle) {
        URI url = URI.create(urlBase);
        return new IsbnProperties(TIMEOUT, Duration.ofHours(24),
                new IsbnProperties.BrasilApi(url),
                new IsbnProperties.GoogleBooks(url, chaveGoogle),
                new IsbnProperties.OpenLibrary(url, url));
    }

    public static IsbnProperties apontandoPara(String urlBase) {
        return apontandoPara(urlBase, null);
    }
}
