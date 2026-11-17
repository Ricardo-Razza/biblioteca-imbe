package br.gov.imbe.biblioteca.acervo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    Optional<Exemplar> findByTombo(String tombo);

    boolean existsByTombo(String tombo);

    List<Exemplar> findByObraIdOrderByTombo(Long obraId);

    /** {@code SELECT ... FOR UPDATE}: empréstimo e reserva do mesmo exemplar não passam ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exemplar e where e.tombo = :tombo")
    Optional<Exemplar> travarPorTombo(@Param("tombo") String tombo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exemplar e where e.id = :id")
    Optional<Exemplar> travar(@Param("id") Long id);
}
