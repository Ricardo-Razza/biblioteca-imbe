package br.gov.imbe.biblioteca.emprestimo;

/** Filtro e situação calculada. Como filtro, {@code ATIVO} inclui os atrasados (todos em aberto). */
public enum StatusEmprestimo {
    ATIVO,
    ATRASADO,
    DEVOLVIDO
}
