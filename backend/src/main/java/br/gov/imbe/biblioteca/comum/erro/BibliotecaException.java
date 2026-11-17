package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/**
 * Base dos erros esperados da aplicação. A mensagem é mostrada ao usuário, então deve estar em português
 * e não pode conter dados pessoais (CPF, data de nascimento).
 */
public abstract sealed class BibliotecaException extends RuntimeException
        permits RecursoNaoEncontradoException, RegraNegocioException, ConflitoException {

    private final HttpStatus status;
    private final String titulo;

    protected BibliotecaException(HttpStatus status, String titulo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }
}
