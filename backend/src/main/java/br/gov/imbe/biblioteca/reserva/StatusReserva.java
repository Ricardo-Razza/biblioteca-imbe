package br.gov.imbe.biblioteca.reserva;

/** {@code AGUARDANDO_RETIRADA} prende o exemplar; os outros são finais. */
public enum StatusReserva {
    AGUARDANDO_RETIRADA,
    RETIRADA,
    CANCELADA,
    EXPIRADA
}
