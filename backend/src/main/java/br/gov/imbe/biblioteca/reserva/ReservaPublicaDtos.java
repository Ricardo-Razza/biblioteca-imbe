package br.gov.imbe.biblioteca.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs do site público. CPF e data de nascimento só chegam no corpo do POST e nunca voltam na resposta,
 * que mostra só o primeiro nome do leitor.
 */
public final class ReservaPublicaDtos {

    private ReservaPublicaDtos() {
    }

    public record Identificacao(
            @NotBlank(message = "Informe o CPF.") @Size(max = 20, message = "CPF inválido.") String cpf,
            @NotNull(message = "Informe a data de nascimento.") LocalDate dataNascimento) {
    }

    /**
     * @param nome     obrigatório sempre; só é usado quando o CPF ainda não é leitor (pré-cadastro)
     * @param telefone obrigatório: a equipe avisa por ele que o livro está separado. Com DDD, 10 ou 11 dígitos.
     * @param email    opcional
     */
    public record NovaReserva(
            @NotNull(message = "Escolha o livro.") Long obraId,
            @NotBlank(message = "Informe o nome completo.") @Size(max = 200, message = "Use no máximo 200 caracteres.")
            String nome,
            @NotBlank(message = "Informe o CPF.") @Size(max = 20, message = "CPF inválido.") String cpf,
            @NotNull(message = "Informe a data de nascimento.") LocalDate dataNascimento,
            @NotBlank(message = "Informe um telefone para contato.")
            @Pattern(regexp = "[\\d\\s()+.-]{10,20}", message = "Telefone inválido. Use o DDD, ex.: (51) 99999-9999.")
            String telefone,
            @Email(message = "E-mail inválido.") @Size(max = 150, message = "Use no máximo 150 caracteres.")
            String email) {
    }

    /** Uma reserva vista pelo próprio leitor. */
    public record ReservaPublica(
            String protocolo,
            StatusReserva status,
            Long obraId,
            String titulo,
            List<String> autores,
            boolean temCapa,
            String primeiroNome,
            LocalDateTime criadaEm,
            LocalDate prazoRetirada,
            LocalDateTime finalizadaEm) {

        static ReservaPublica de(Reserva r) {
            return new ReservaPublica(r.getProtocolo(), r.getStatus(), r.getObra().getId(), r.getObra().getTitulo(),
                    ReservaResposta.autores(r.getObra()), r.getObra().getCapaArquivo() != null,
                    r.getLeitor().primeiroNome(), r.getCriadaEm(), r.getPrazoRetirada(), r.getFinalizadaEm());
        }
    }

    /** "Minhas reservas": as 20 mais recentes, da mais nova para a mais antiga. */
    public record MinhasReservas(String primeiroNome, List<ReservaPublica> reservas) {
    }
}
