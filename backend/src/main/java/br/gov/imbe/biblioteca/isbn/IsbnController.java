package br.gov.imbe.biblioteca.isbn;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.acervo.CapaService;
import br.gov.imbe.biblioteca.acervo.ObraService;

/** Busca por ISBN nas fontes externas. Perfil: Atendente (a partir da fase 3). */
@RestController
@RequestMapping("/api/v1/isbn")
public class IsbnController {

    private final IsbnLookupService servico;
    private final ObraService obras;
    private final CapaService capas;

    public IsbnController(IsbnLookupService servico, ObraService obras, CapaService capas) {
        this.servico = servico;
        this.obras = obras;
        this.capas = capas;
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
                .orElseGet(() -> {
                    ResultadoIsbn r = servico.buscar(valido);
                    // Baixa já aqui (fica em cache para a prévia e o cadastro): só diz que tem capa se uma serviu.
                    boolean capa = r.encontrado() && capas.previaExterna(valido).isPresent();
                    return IsbnResposta.de(r, capa);
                });
    }

    /**
     * Prévia da capa indicada pelas fontes, para o formulário de conferência (quando {@code capaDisponivel}).
     * O backend baixa e devolve a imagem já processada: a tela nunca usa o link externo. 404 se não houver.
     */
    @GetMapping("/{isbn}/capa")
    public ResponseEntity<byte[]> capa(@PathVariable String isbn, @RequestParam(required = false) String tamanho) {
        boolean mini = "mini".equals(tamanho);
        return capas.previaExterna(Isbn.de(isbn))
                .map(c -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                        .body(mini ? c.miniatura() : c.grande()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
