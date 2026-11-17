package br.gov.imbe.biblioteca.emprestimo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    @Query("select count(e) from Emprestimo e where e.leitor.id = :leitorId and e.dataDevolucao is null")
    long contarEmAberto(@Param("leitorId") Long leitorId);

    @Query("""
            select count(e) from Emprestimo e
            where e.leitor.id = :leitorId and e.dataDevolucao is null and e.dataPrevista < :hoje
            """)
    long contarAtrasados(@Param("leitorId") Long leitorId, @Param("hoje") LocalDate hoje);

    @Query("select e from Emprestimo e where e.exemplar.id = :exemplarId and e.dataDevolucao is null")
    Optional<Emprestimo> emAbertoDoExemplar(@Param("exemplarId") Long exemplarId);

    @Query("""
            select e from Emprestimo e
            where e.leitor.id = :leitorId and e.dataDevolucao is null
            order by e.dataPrevista
            """)
    List<Emprestimo> emAbertoDoLeitor(@Param("leitorId") Long leitorId);

    /**
     * Filtros opcionais (nulo = sem filtro). {@code situacao}: 0 em aberto, 1 atrasados, 2 devolvidos.
     * {@code padrao} procura no título da obra e no nome do leitor.
     */
    @Query(value = """
            select e from Emprestimo e join e.exemplar x join x.obra o join e.leitor l
            where (:situacao = 0 and e.dataDevolucao is null
                or :situacao = 1 and e.dataDevolucao is null and e.dataPrevista < :hoje
                or :situacao = 2 and e.dataDevolucao is not null)
              and (:leitorId is null or l.id = :leitorId)
              and (:tombo is null or x.tombo = :tombo)
              and (:padrao is null or o.titulo like :padrao escape '!' or l.nome like :padrao escape '!')
            """, countQuery = """
            select count(e) from Emprestimo e join e.exemplar x join x.obra o join e.leitor l
            where (:situacao = 0 and e.dataDevolucao is null
                or :situacao = 1 and e.dataDevolucao is null and e.dataPrevista < :hoje
                or :situacao = 2 and e.dataDevolucao is not null)
              and (:leitorId is null or l.id = :leitorId)
              and (:tombo is null or x.tombo = :tombo)
              and (:padrao is null or o.titulo like :padrao escape '!' or l.nome like :padrao escape '!')
            """)
    Page<Emprestimo> buscar(@Param("situacao") int situacao, @Param("hoje") LocalDate hoje,
            @Param("leitorId") Long leitorId, @Param("tombo") String tombo, @Param("padrao") String padrao,
            Pageable paginacao);
}
