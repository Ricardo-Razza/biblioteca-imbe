package br.gov.imbe.biblioteca.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    public record NovaReserva(
            @NotNull(message = "Escolha o livro.") Long obraId,
            @NotBlank(message = "Informe o CPF.") @Size(max = 20, message = "CPF inválido.") String cpf,
            @NotNull(message = "Informe a data de nascimento.") LocalDate dataNascimento) {
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
