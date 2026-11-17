package br.gov.imbe.biblioteca.isbn;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Busca por ISBN nas fontes externas. Perfil: Atendente (a partir da fase 3). */
@RestController
@RequestMapping("/api/v1/isbn")
public class IsbnController {

    private final IsbnLookupService servico;

    public IsbnController(IsbnLookupService servico) {
        this.servico = servico;
    }

    /** Aceita ISBN-10 ou ISBN-13, com ou sem hífens. ISBN inválido: 400 com o motivo. */
    @GetMapping("/{isbn}")
    public IsbnResposta buscar(@PathVariable String isbn) {
        return IsbnResposta.de(servico.buscar(Isbn.de(isbn)));
    }
}
