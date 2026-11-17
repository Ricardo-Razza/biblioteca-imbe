package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 404: o recurso pedido não existe (ex.: obra, exemplar pelo tombo). */
public final class RecursoNaoEncontradoException extends BibliotecaException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, "Não encontrado", mensagem);
    }
}
