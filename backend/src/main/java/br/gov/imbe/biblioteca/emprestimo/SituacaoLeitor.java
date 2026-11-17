package br.gov.imbe.biblioteca.emprestimo;

import java.time.LocalDate;
import java.util.List;

/**
 * O que o balcão precisa saber antes de emprestar.
 *
 * @param livrosEmUso empréstimos em aberto + reservas aguardando retirada (contam juntos no limite)
 * @param motivos     por que não pode emprestar; vazio quando pode
 */
public record SituacaoLeitor(
        Long leitorId,
        String nome,
        boolean ativo,
        LocalDate bloqueadoAte,
        long emprestimosEmAberto,
        long atrasados,
        long reservasAguardando,
        long livrosEmUso,
        int limite,
        boolean podeEmprestar,
        List<String> motivos) {
}
