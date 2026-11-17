package br.gov.imbe.biblioteca.comum.erro;

import org.springframework.http.HttpStatus;

/** 400: o valor enviado é inválido por si só (ex.: ISBN com dígito verificador errado). */
public final class RequisicaoInvalidaException extends BibliotecaException {

    public RequisicaoInvalidaException(String titulo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, titulo, mensagem);
    }
}
