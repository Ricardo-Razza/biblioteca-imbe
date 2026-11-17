package br.gov.imbe.biblioteca.acervo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    /** Compara pela collation do banco: sem diferenciar maiúsculas nem acentos. */
    Optional<Autor> findByNome(String nome);
}
