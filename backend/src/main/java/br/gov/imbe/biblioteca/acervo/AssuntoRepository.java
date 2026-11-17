package br.gov.imbe.biblioteca.acervo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssuntoRepository extends JpaRepository<Assunto, Long> {

    /** Compara pela collation do banco: sem diferenciar maiúsculas nem acentos. */
    Optional<Assunto> findByNome(String nome);
}
