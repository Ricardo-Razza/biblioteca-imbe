package br.gov.imbe.biblioteca.isbn;

import org.springframework.web.client.RestClientException;

/** A fonte externa não respondeu direito. Uso interno do módulo: não chega ao usuário como erro. */
public class FonteIsbnIndisponivelException extends RuntimeException {

    private final FonteIsbn fonte;

    public FonteIsbnIndisponivelException(FonteIsbn fonte, String motivo, Throwable causa) {
        super(fonte + ": " + motivo, causa);
        this.fonte = fonte;
    }

    /**
     * Converte uma falha do RestClient sem levar a URL junto: ela pode conter a chave da API (Google Books).
     * Guarda só a causa mais específica (timeout, conexão recusada, JSON ilegível).
     */
    public static FonteIsbnIndisponivelException de(FonteIsbn fonte, RestClientException e) {
        Throwable causa = e.getMostSpecificCause();
        String motivo = causa == e ? e.getClass().getSimpleName()
                : causa.getClass().getSimpleName() + (causa.getMessage() == null ? "" : " (" + causa.getMessage() + ")");
        return new FonteIsbnIndisponivelException(fonte, motivo, causa == e ? null : causa);
    }

    public FonteIsbn getFonte() {
        return fonte;
    }
}
