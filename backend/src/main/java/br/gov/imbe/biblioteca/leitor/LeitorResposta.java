package br.gov.imbe.biblioteca.leitor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Leitor na gestão. O CPF sai completo só aqui; o site público nunca devolve dados de leitor. */
public record LeitorResposta(
        Long id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String telefone,
        String email,
        String endereco,
        String responsavel,
        boolean ativo,
        boolean menorDeIdade,
        LocalDate bloqueadoAte,
        LocalDateTime criadoEm) {

    public static LeitorResposta de(Leitor l, LocalDate hoje) {
        return new LeitorResposta(l.getId(), l.getNome(), l.getCpf(), l.getDataNascimento(), l.getTelefone(),
                l.getEmail(), l.getEndereco(), l.getResponsavel(), l.isAtivo(), l.menorDeIdadeEm(hoje),
                l.bloqueadoEm(hoje) ? l.getBloqueadoAte() : null, l.getCriadoEm());
    }
}
