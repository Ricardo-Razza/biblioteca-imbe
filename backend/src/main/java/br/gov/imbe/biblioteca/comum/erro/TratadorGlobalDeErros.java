package br.gov.imbe.biblioteca.comum.erro;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte todos os erros em {@link ProblemDetail} (RFC 9457) com mensagens em português.
 * Os erros do próprio Spring MVC (rota inexistente, JSON inválido etc.) são traduzidos em {@code messages.properties}.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    /** Um campo inválido no corpo da requisição. */
    public record CampoInvalido(String campo, String mensagem) {
    }

    @ExceptionHandler(BibliotecaException.class)
    ProblemDetail tratarBiblioteca(BibliotecaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problema.setTitle(ex.getTitulo());
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Há campos inválidos. Corrija e tente novamente.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("campos", campos);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Chave única ou estrangeira violada no banco. Acontece em corrida (dois cadastros do mesmo ISBN ou tombo
     * ao mesmo tempo); o caso normal já é checado antes no service, com mensagem específica.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Os dados conflitam com um registro já cadastrado. Atualize a tela e confira.");
        problema.setTitle("Conflito");
        return problema;
    }

    /** {@code ?sort=} com campo que não existe. */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail tratarOrdenacao(PropertyReferenceException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Não é possível ordenar por " + ex.getPropertyName() + ".");
        problema.setTitle("Parâmetro inválido");
        return problema;
    }

    /** Erro inesperado: registra no log e devolve mensagem genérica, sem detalhes internos. */
    @ExceptionHandler(Exception.class)
    ProblemDetail tratarInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente; se continuar, avise a equipe de TI.");
        problema.setTitle("Erro interno");
        return problema;
    }
}
