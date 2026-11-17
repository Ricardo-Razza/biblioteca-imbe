package br.gov.imbe.biblioteca.funcionario;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioAlteracao;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioRequisicao;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioResposta;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.SenhaProvisoriaResposta;
import jakarta.validation.Valid;

/** Perfil: Admin. */
@RestController
@RequestMapping("/api/v1/funcionarios")
public class FuncionarioController {

    private final FuncionarioService servico;

    public FuncionarioController(FuncionarioService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<FuncionarioResposta> listar() {
        return servico.listar();
    }

    @PostMapping
    public ResponseEntity<SenhaProvisoriaResposta> cadastrar(@Valid @RequestBody FuncionarioRequisicao req) {
        SenhaProvisoriaResposta r = servico.cadastrar(req);
        return ResponseEntity.created(URI.create("/api/v1/funcionarios/" + r.funcionario().id())).body(r);
    }

    @PutMapping("/{id}")
    public FuncionarioResposta alterar(@PathVariable Long id, @Valid @RequestBody FuncionarioAlteracao alt) {
        return servico.alterar(id, alt);
    }

    @PostMapping("/{id}/senha-provisoria")
    public SenhaProvisoriaResposta redefinirSenha(@PathVariable Long id) {
        return servico.redefinirSenha(id);
    }
}
