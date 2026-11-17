package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 503: um serviço de que a operação depende (ex.: a API de login da Prefeitura) não respondeu. */
public final class IndisponivelException extends BibliotecaException {

    public IndisponivelException(String mensagem) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível", mensagem);
    }
}
