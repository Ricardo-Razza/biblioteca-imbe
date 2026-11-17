package br.gov.imbe.biblioteca.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.auth.AuthDtos.FuncionarioLogado;
import br.gov.imbe.biblioteca.auth.AuthDtos.LoginRequisicao;
import br.gov.imbe.biblioteca.auth.AuthDtos.LoginResposta;
import br.gov.imbe.biblioteca.auth.AuthDtos.TrocaSenhaRequisicao;
import jakarta.validation.Valid;

/** Login (público), quem sou eu e troca de senha (qualquer funcionário logado, inclusive com senha provisória). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService servico;

    public AuthController(AuthService servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao req) {
        return servico.login(req);
    }

    @GetMapping("/eu")
    public FuncionarioLogado eu() {
        return servico.eu();
    }

    @PostMapping("/senha")
    public FuncionarioLogado trocarSenha(@Valid @RequestBody TrocaSenhaRequisicao req) {
        return servico.trocarSenha(req);
    }
}
