package br.gov.imbe.biblioteca.publico;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.acervo.SituacaoExemplar;

/**
 * Consultas do catálogo público. Só aparece obra com algum exemplar em circulação ({@code visiveis}: disponível,
 * reservado ou emprestado); obra sem exemplar, só com extraviados ou baixados, fica de fora.
 */
interface CatalogoRepository extends Repository<Obra, Long> {

    /**
     * {@code padrao} (com {@code % _ !} escapados por {@code !}) procura no título, subtítulo, autores e assuntos.
     * A collation do banco ignora maiúsculas e acentos.
     */
    @Query(value = """
            select o from Obra o
            where exists (select 1 from Exemplar x where x.obra = o and x.situacao in :visiveis)
              and (:padrao is null
                   or o.titulo like :padrao escape '!'
                   or o.subtitulo like :padrao escape '!'
                   or exists (select 1 from ObraAutor oa where oa.obra = o and oa.autor.nome like :padrao escape '!')
                   or exists (select 1 from Obra o2 join o2.assuntos s where o2 = o and s.nome like :padrao escape '!'))
              and (:assuntoId is null
                   or exists (select 1 from Obra o3 join o3.assuntos s3 where o3 = o and s3.id = :assuntoId))
              and (:soDisponiveis = false
                   or exists (select 1 from Exemplar d where d.obra = o and d.situacao = :disponivel))
            """, countQuery = """
            select count(o) from Obra o
            where exists (select 1 from Exemplar x where x.obra = o and x.situacao in :visiveis)
              and (:padrao is null
                   or o.titulo like :padrao escape '!'
                   or o.subtitulo like :padrao escape '!'
                   or exists (select 1 from ObraAutor oa where oa.obra = o and oa.autor.nome like :padrao escape '!')
                   or exists (select 1 from Obra o2 join o2.assuntos s where o2 = o and s.nome like :padrao escape '!'))
              and (:assuntoId is null
                   or exists (select 1 from Obra o3 join o3.assuntos s3 where o3 = o and s3.id = :assuntoId))
              and (:soDisponiveis = false
                   or exists (select 1 from Exemplar d where d.obra = o and d.situacao = :disponivel))
            """)
    Page<Obra> buscar(@Param("visiveis") Collection<SituacaoExemplar> visiveis, @Param("padrao") String padrao,
            @Param("assuntoId") Long assuntoId, @Param("soDisponiveis") boolean soDisponiveis,
            @Param("disponivel") SituacaoExemplar disponivel, Pageable paginacao);

    @Query("""
            select o from Obra o
            where o.id = :id and exists (select 1 from Exemplar x where x.obra = o and x.situacao in :visiveis)
            """)
    Optional<Obra> visivel(@Param("id") Long id, @Param("visiveis") Collection<SituacaoExemplar> visiveis);

    /**
     * Autores das obras visíveis mais emprestadas desde {@code desde}, do mais retirado para o menos. Só o nome:
     * nada de leitor nem de empréstimo sai daqui.
     */
    @Query("""
            select a.nome from Emprestimo e
              join e.exemplar x join x.obra o join o.autores oa join oa.autor a
            where e.dataSaida >= :desde
              and exists (select 1 from Exemplar v where v.obra = o and v.situacao in :visiveis)
            group by a.id, a.nome
            order by count(e) desc, a.nome
            """)
    List<String> autoresMaisRetirados(@Param("desde") LocalDate desde,
            @Param("visiveis") Collection<SituacaoExemplar> visiveis, Pageable limite);

    /** Assuntos com alguma obra visível, com a quantidade de obras, em ordem alfabética. */
    @Query("""
            select new br.gov.imbe.biblioteca.publico.AssuntoPublico(s.id, s.nome, count(distinct o.id))
            from Obra o join o.assuntos s
            where exists (select 1 from Exemplar x where x.obra = o and x.situacao in :visiveis)
            group by s.id, s.nome
            order by s.nome
            """)
    List<AssuntoPublico> assuntos(@Param("visiveis") Collection<SituacaoExemplar> visiveis);
}
