package br.gov.imbe.biblioteca.funcionario;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpos e respostas de {@code /api/v1/funcionarios}. */
public final class FuncionarioDtos {

    private FuncionarioDtos() {
    }

    public record FuncionarioRequisicao(
            @NotBlank(message = "Informe o nome.") @Size(max = 150, message = "Nome muito longo.") String nome,
            @NotBlank(message = "Informe o login.")
            @Pattern(regexp = "\\s*[A-Za-z0-9._-]{3,60}\\s*",
                    message = "Use de 3 a 60 letras sem acento, números, ponto, hífen ou sublinhado.") String login,
            @NotNull(message = "Escolha como o funcionário entra.") TipoAutenticacao tipo,
            @NotNull(message = "Escolha o perfil.") Perfil perfil) {
    }

    public record FuncionarioAlteracao(
            @NotBlank(message = "Informe o nome.") @Size(max = 150, message = "Nome muito longo.") String nome,
            @NotNull(message = "Escolha o perfil.") Perfil perfil,
            @NotNull(message = "Informe se está ativo.") Boolean ativo) {
    }

    public record FuncionarioResposta(Long id, String nome, String login, TipoAutenticacao tipo, Perfil perfil,
            boolean ativo, boolean trocarSenha, LocalDateTime ultimoAcesso, LocalDateTime loginTravadoAte,
            LocalDateTime criadoEm) {

        static FuncionarioResposta de(Funcionario f, LocalDateTime agora) {
            return new FuncionarioResposta(f.getId(), f.getNome(), f.getLogin(), f.getTipo(), f.getPerfil(),
                    f.isAtivo(), f.isTrocarSenha(), f.getUltimoAcesso(),
                    f.loginBloqueadoEm(agora) ? f.getBloqueadoAte() : null, f.getCriadoEm());
        }
    }

    /** {@code senhaProvisoria} só vem para usuário LOCAL e só nesta resposta: não fica guardada em texto. */
    public record SenhaProvisoriaResposta(FuncionarioResposta funcionario, String senhaProvisoria) {
    }
}
