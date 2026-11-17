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
            where o.titulo like :padrao escape '!'
               or exists (select 1 from ObraAutor oa where oa.obra = o and oa.autor.nome like :padrao escape '!')
            """)
    Page<Obra> buscarPorTituloOuAutor(@Param("padrao") String padrao, Pageable paginacao);
}
