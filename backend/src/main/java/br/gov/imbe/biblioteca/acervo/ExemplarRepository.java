package br.gov.imbe.biblioteca.acervo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    Optional<Exemplar> findByTombo(String tombo);

    boolean existsByTombo(String tombo);

    List<Exemplar> findByObraIdOrderByTombo(Long obraId);

    /**
     * Busca do balcão: tombo exato, ISBN exato ou trecho do título ou de um autor, só nas situações pedidas.
     * O tombo exato vem primeiro; depois, por título e tombo.
     *
     * @param padrao trecho com curingas e {@code % _ !} escapados por {@code !}
     */
    @Query("""
            select e from Exemplar e join e.obra o
            where e.situacao in :situacoes
              and (e.tombo = :tombo or o.isbn13 = :isbn or o.titulo like :padrao escape '!'
                   or exists (select 1 from ObraAutor oa where oa.obra = o and oa.autor.nome like :padrao escape '!'))
            order by case when e.tombo = :tombo then 0 else 1 end, o.titulo, e.tombo
            """)
    List<Exemplar> buscarNoBalcao(@Param("tombo") String tombo, @Param("isbn") String isbn,
            @Param("padrao") String padrao, @Param("situacoes") Collection<SituacaoExemplar> situacoes,
            Pageable limite);

    /** {@code SELECT ... FOR UPDATE}: empréstimo e reserva do mesmo exemplar não passam ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exemplar e where e.tombo = :tombo")
    Optional<Exemplar> travarPorTombo(@Param("tombo") String tombo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exemplar e where e.id = :id")
    Optional<Exemplar> travar(@Param("id") Long id);

    /**
     * Prende um exemplar disponível da obra para a reserva. {@code SKIP LOCKED}: se outra reserva está prendendo
     * um exemplar agora, pega o próximo em vez de esperar; sem nenhum livre, volta vazio. Assim dois cidadãos
     * não reservam a última cópia.
     */
    @Query(value = """
            select * from exemplar
            where obra_id = :obraId and situacao = 'DISPONIVEL'
            order by id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<Exemplar> travarDisponivelDaObra(@Param("obraId") Long obraId);
}
