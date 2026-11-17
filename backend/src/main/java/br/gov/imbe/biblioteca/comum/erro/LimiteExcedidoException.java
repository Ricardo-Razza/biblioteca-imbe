package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 429: muitas requisições seguidas do mesmo IP ou para o mesmo CPF no site público. */
public final class LimiteExcedidoException extends BibliotecaException {

    public LimiteExcedidoException(String mensagem) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas", mensagem);
    }
}
