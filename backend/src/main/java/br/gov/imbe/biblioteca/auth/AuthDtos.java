package br.gov.imbe.biblioteca.auth;

import java.time.Instant;

import br.gov.imbe.biblioteca.funcionario.Funcionario;
import br.gov.imbe.biblioteca.funcionario.Perfil;
import br.gov.imbe.biblioteca.funcionario.TipoAutenticacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpos e respostas de {@code /api/v1/auth}. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequisicao(
            @NotBlank(message = "Informe o login.") @Size(max = 60, message = "Login muito longo.") String login,
            @NotBlank(message = "Informe a senha.") @Size(max = 200, message = "Senha muito longa.") String senha) {
    }

    public record TrocaSenhaRequisicao(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank(message = "Informe a nova senha.") String novaSenha) {
    }

    /** Quem está logado; o front usa o perfil para mostrar ou esconder ações. */
    public record FuncionarioLogado(Long id, String nome, String login, TipoAutenticacao tipo, Perfil perfil,
            boolean trocarSenha) {

        public static FuncionarioLogado de(Funcionario f) {
            return new FuncionarioLogado(f.getId(), f.getNome(), f.getLogin(), f.getTipo(), f.getPerfil(),
                    f.isTrocarSenha());
        }
    }

    public record LoginResposta(String token, Instant expiraEm, FuncionarioLogado funcionario) {
    }
}
