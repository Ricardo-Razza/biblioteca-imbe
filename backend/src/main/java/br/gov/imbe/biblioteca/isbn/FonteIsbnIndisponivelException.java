package br.gov.imbe.biblioteca.isbn;

/** A fonte externa não respondeu direito. Uso interno do módulo: não chega ao usuário como erro. */
public class FonteIsbnIndisponivelException extends RuntimeException {

    private final FonteIsbn fonte;

    public FonteIsbnIndisponivelException(FonteIsbn fonte, String motivo, Throwable causa) {
        super(fonte + ": " + motivo, causa);
        this.fonte = fonte;
    }

    public FonteIsbn getFonte() {
        return fonte;
    }
}
