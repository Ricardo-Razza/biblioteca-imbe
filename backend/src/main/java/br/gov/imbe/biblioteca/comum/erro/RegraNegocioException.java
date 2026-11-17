package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 422: a requisição é válida, mas uma regra de negócio impede a operação (ex.: leitor com atraso). */
public final class RegraNegocioException extends BibliotecaException {

    public RegraNegocioException(String mensagem) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Operação não permitida", mensagem);
    }
}
