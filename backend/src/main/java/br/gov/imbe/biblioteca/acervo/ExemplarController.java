package br.gov.imbe.biblioteca.acervo;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Exemplares na gestão. Cadastro e consulta: Atendente; alteração: Admin (a partir da fase 3).
 * Exemplar não se apaga: a saída do acervo é a situação {@code BAIXADO}.
 */
@RestController
@RequestMapping("/api/v1")
public class ExemplarController {

    private final ExemplarService servico;

    public ExemplarController(ExemplarService servico) {
        this.servico = servico;
    }

    @PostMapping("/obras/{obraId}/exemplares")
    public ResponseEntity<ExemplarResposta> cadastrar(@PathVariable Long obraId,
            @Valid @RequestBody ExemplarRequisicao req) {
        ExemplarResposta exemplar = servico.cadastrar(obraId, req);
        return ResponseEntity.created(URI.create("/api/v1/exemplares/" + exemplar.id())).body(exemplar);
    }

    @GetMapping("/obras/{obraId}/exemplares")
    public List<ExemplarResposta> daObra(@PathVariable Long obraId) {
        return servico.daObra(obraId);
    }

    /** Balcão: exemplares que podem sair, por tombo, ISBN, título ou autor (até 20, tombo exato primeiro). */
    @GetMapping("/exemplares")
    public List<ExemplarResposta> buscarParaEmprestimo(@RequestParam(name = "q", required = false) String termo) {
        return servico.buscarParaEmprestimo(termo);
    }

    /** Leitura da etiqueta no balcão. Não diferencia maiúsculas. */
    @GetMapping("/exemplares/tombo/{tombo}")
    public ExemplarResposta porTombo(@PathVariable String tombo) {
        return servico.porTombo(tombo);
    }

    @PatchMapping("/exemplares/{id}")
    public ExemplarResposta alterar(@PathVariable Long id, @Valid @RequestBody ExemplarAlteracao alt) {
        return servico.alterar(id, alt);
    }
}
