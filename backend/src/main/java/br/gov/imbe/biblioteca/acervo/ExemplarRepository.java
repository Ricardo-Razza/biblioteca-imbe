package br.gov.imbe.biblioteca.acervo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    Optional<Exemplar> findByTombo(String tombo);

    boolean existsByTombo(String tombo);

    List<Exemplar> findByObraIdOrderByTombo(Long obraId);
}
