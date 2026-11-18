package br.gov.imbe.biblioteca.acervo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObraRepository extends JpaRepository<Obra, Long> {

    Optional<Obra> findByIsbn13(String isbn13);

    boolean existsByIsbn13(String isbn13);

    boolean existsByIsbn13AndIdNot(String isbn13, Long id);

    /**
     * Busca por trecho do título ou do nome de um autor. A collation do banco já ignora maiúsculas e acentos.
     *
     * @param padrao trecho com curingas, ex.: {@code %saramago%}, com {@code % _ !} do usuário escapados por
     *               {@code !} (a barra invertida é escape dentro de literais no MySQL e quebraria o SQL)
     */
    @Query("""
            select o from Obra o
            where (o.titulo like :padrao escape '!'
               or exists (select 1 from ObraAutor oa where oa.obra = o and oa.autor.nome like :padrao escape '!'))
              and (:semCapa = false or o.capaArquivo is null)
            """)
    Page<Obra> buscarPorTituloOuAutor(@Param("padrao") String padrao, @Param("semCapa") boolean semCapa,
            Pageable paginacao);

    Page<Obra> findByCapaArquivoIsNull(Pageable paginacao);

    /** Para a busca semanal de capas: id e ISBN das obras sem capa, das mais antigas para as mais novas. */
    @Query("select o.id as id, o.isbn13 as isbn13 from Obra o where o.capaArquivo is null order by o.id")
    java.util.List<SemCapa> semCapa(Pageable limite);

    interface SemCapa {
        Long getId();

        String getIsbn13();
    }
}
