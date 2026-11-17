package br.gov.imbe.biblioteca.leitor;

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

/** Leitores na gestão. Perfil: Atendente; desbloqueio: Admin. Leitor não se apaga. */
@RestController
@RequestMapping("/api/v1/leitores")
public class LeitorController {

    private final LeitorService servico;

    public LeitorController(LeitorService servico) {
        this.servico = servico;
    }

    /** {@code ?q=} trecho do nome ou começo do CPF. */
    @GetMapping
    public Pagina<LeitorResposta> listar(@RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return servico.listar(q, paginacao);
    }

    @GetMapping("/{id}")
    public LeitorResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<LeitorResposta> cadastrar(@Valid @RequestBody LeitorRequisicao req) {
        LeitorResposta leitor = servico.cadastrar(req);
        return ResponseEntity.created(URI.create("/api/v1/leitores/" + leitor.id())).body(leitor);
    }

    @PutMapping("/{id}")
    public LeitorResposta alterar(@PathVariable Long id, @Valid @RequestBody LeitorRequisicao req) {
        return servico.alterar(id, req);
    }

    @PostMapping("/{id}/desbloqueio")
    public LeitorResposta desbloquear(@PathVariable Long id) {
        return servico.desbloquear(id);
    }
}
