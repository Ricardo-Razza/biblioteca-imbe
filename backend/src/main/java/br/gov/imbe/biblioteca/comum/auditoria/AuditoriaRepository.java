package br.gov.imbe.biblioteca.comum.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /** Mais recentes primeiro, com o nome de quem fez (a tabela de funcionários fica em outro módulo). */
    @Query(value = """
            select new br.gov.imbe.biblioteca.comum.auditoria.AuditoriaResposta(
                a.id, a.dataHora, a.funcionarioId, f.nome, a.acao, a.entidade, a.entidadeId, a.detalhe)
            from RegistroAuditoria a left join Funcionario f on f.id = a.funcionarioId
            where (:acao is null or a.acao = :acao)
            order by a.dataHora desc, a.id desc
            """, countQuery = "select count(a) from RegistroAuditoria a where (:acao is null or a.acao = :acao)")
    Page<AuditoriaResposta> listar(AcaoAuditoria acao, Pageable paginacao);
}
