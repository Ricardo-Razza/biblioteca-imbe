package br.gov.imbe.biblioteca.acervo;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.Pagina;
import jakarta.validation.Valid;

/** Acervo na gestão. Perfil: Atendente (a partir da fase 3). Obra não se apaga. */
@RestController
@RequestMapping("/api/v1/obras")
public class ObraController {

    private final ObraService servico;

    public ObraController(ObraService servico) {
        this.servico = servico;
    }

    /** {@code ?q=} busca por ISBN, trecho do título ou do autor. Paginação: {@code ?page=0&size=20}. */
    @GetMapping
    public Pagina<ObraResposta> listar(@RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable paginacao) {
        return servico.listar(q, paginacao);
    }

    @GetMapping("/{id}")
    public ObraResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<ObraResposta> cadastrar(@Valid @RequestBody ObraRequisicao req) {
        ObraResposta obra = servico.cadastrar(req);
        return ResponseEntity.created(URI.create("/api/v1/obras/" + obra.id())).body(obra);
    }

    @PutMapping("/{id}")
    public ObraResposta alterar(@PathVariable Long id, @Valid @RequestBody ObraRequisicao req) {
        return servico.alterar(id, req);
    }
}
