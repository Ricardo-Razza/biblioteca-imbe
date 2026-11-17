package br.gov.imbe.biblioteca.emprestimo;

import java.util.Optional;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.leitor.Leitor;

/**
 * O que o balcão precisa do módulo de reserva, sem depender dele: reservas aguardando retirada contam no limite
 * de livros do leitor, e o exemplar reservado bipado no balcão para o próprio leitor vira a retirada.
 * Sem o módulo, conta zero e exemplar reservado não sai.
 */
public interface ReservasAtivas {

    int contar(Long leitorId);

    /**
     * Exemplar {@code RESERVADO}, já travado, bipado no balcão para {@code leitor}. Se a reserva é desse leitor,
     * registra a retirada e devolve o empréstimo aberto; se é de outro, devolve vazio.
     */
    Optional<Emprestimo> retirarNoBalcao(Exemplar exemplar, Leitor leitor, Long funcionarioId);
}
