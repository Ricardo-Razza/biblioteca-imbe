package br.gov.imbe.biblioteca.emprestimo;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import jakarta.validation.Valid;

/** Balcão. Perfil: Atendente. */
@RestController
@RequestMapping("/api/v1")
public class EmprestimoController {

    private final EmprestimoService servico;

    public EmprestimoController(EmprestimoService servico) {
        this.servico = servico;
    }

    /** Antes de emprestar: limite, atrasos, bloqueio e os motivos quando não pode. */
    @GetMapping("/leitores/{id}/situacao")
    public SituacaoLeitor situacao(@PathVariable Long id) {
        return servico.situacao(id);
    }

    @PostMapping("/emprestimos")
    public ResponseEntity<EmprestimoResposta> emprestar(@Valid @RequestBody EmprestimoRequisicao req) {
        EmprestimoResposta e = servico.emprestar(req, UsuarioAtual.idOuNulo());
        return ResponseEntity.created(URI.create("/api/v1/emprestimos/" + e.id())).body(e);
    }

    /**
     * {@code ?status=ATIVO} (padrão, todos em aberto), {@code ATRASADO} ou {@code DEVOLVIDO};
     * {@code leitorId}, {@code tombo} e {@code q} (título ou nome do leitor) opcionais.
     */
    @GetMapping("/emprestimos")
    public Pagina<EmprestimoResposta> listar(@RequestParam(required = false) StatusEmprestimo status,
            @RequestParam(required = false) Long leitorId, @RequestParam(required = false) String tombo,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return servico.listar(status, leitorId, tombo, q, page, size);
    }

    @GetMapping("/emprestimos/{id}")
    public EmprestimoResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping("/emprestimos/{id}/renovacao")
    public EmprestimoResposta renovar(@PathVariable Long id) {
        return servico.renovar(id);
    }

    @PostMapping("/emprestimos/{id}/devolucao")
    public EmprestimoResposta devolver(@PathVariable Long id,
            @Valid @RequestBody(required = false) DevolucaoRequisicao req) {
        return servico.devolver(id, req, UsuarioAtual.idOuNulo());
    }
}
