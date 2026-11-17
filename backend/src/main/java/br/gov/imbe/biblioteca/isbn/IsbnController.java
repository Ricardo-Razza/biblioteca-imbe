package br.gov.imbe.biblioteca.isbn;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.acervo.ObraService;

/** Busca por ISBN nas fontes externas. Perfil: Atendente (a partir da fase 3). */
@RestController
@RequestMapping("/api/v1/isbn")
public class IsbnController {

    private final IsbnLookupService servico;
    private final ObraService obras;

    public IsbnController(IsbnLookupService servico, ObraService obras) {
        this.servico = servico;
        this.obras = obras;
    }

    /**
     * Aceita ISBN-10 ou ISBN-13, com ou sem hífens. ISBN inválido: 400 com o motivo.
     * Obra já cadastrada: devolve o {@code obraId} sem consultar as fontes externas.
     */
    @GetMapping("/{isbn}")
    public IsbnResposta buscar(@PathVariable String isbn) {
        Isbn valido = Isbn.de(isbn);
        return obras.idPorIsbn(valido)
                .map(id -> IsbnResposta.jaCadastrada(valido, id))
                .orElseGet(() -> IsbnResposta.de(servico.buscar(valido)));
    }
}
