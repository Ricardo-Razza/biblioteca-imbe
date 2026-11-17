package br.gov.imbe.biblioteca.importacao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ItemImportacaoRepository extends JpaRepository<ItemImportacao, Long> {

    /** Próximo da fila, na ordem da planilha. */
    Optional<ItemImportacao> findFirstByImportacaoIdAndResultadoOrderByLinha(Long importacaoId,
            ResultadoItem resultado);

    Page<ItemImportacao> findByImportacaoIdAndResultadoInOrderByLinha(Long importacaoId,
            Collection<ResultadoItem> resultados, Pageable paginacao);

    @Query("""
            select i.resultado, count(i) from ItemImportacao i
            where i.importacaoId = :importacaoId group by i.resultado
            """)
    List<Object[]> contar(@Param("importacaoId") Long importacaoId);

    @Modifying
    @Query("""
            update ItemImportacao i set i.resultado = br.gov.imbe.biblioteca.importacao.ResultadoItem.PENDENTE,
                   i.mensagem = null
            where i.importacaoId = :importacaoId
              and i.resultado = br.gov.imbe.biblioteca.importacao.ResultadoItem.FONTE_INDISPONIVEL
            """)
    int voltarIndisponiveisParaFila(@Param("importacaoId") Long importacaoId);
}
