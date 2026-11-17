package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 401: login ou senha incorretos, login travado. Não diz se o login existe. */
public final class NaoAutenticadoException extends BibliotecaException {

    public NaoAutenticadoException(String mensagem) {
        super(HttpStatus.UNAUTHORIZED, "Não autenticado", mensagem);
    }
}
