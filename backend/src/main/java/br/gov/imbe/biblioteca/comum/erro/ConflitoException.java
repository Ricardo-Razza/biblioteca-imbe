package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 409: conflito com o estado atual (ex.: ISBN ou tombo já cadastrado). */
public final class ConflitoException extends BibliotecaException {

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, "Conflito", mensagem);
    }
}
