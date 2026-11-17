package br.gov.imbe.biblioteca.emprestimo;

/**
 * Reservas aguardando retirada contam no limite de livros do leitor. Implementado pelo módulo de reserva;
 * sem ele, conta zero.
 */
public interface ReservasAtivas {

    int contar(Long leitorId);
}
