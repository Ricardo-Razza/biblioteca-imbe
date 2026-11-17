package br.gov.imbe.biblioteca.comum.auditoria;

import java.time.LocalDateTime;

public record AuditoriaResposta(Long id, LocalDateTime dataHora, Long funcionarioId, String funcionario,
        AcaoAuditoria acao, String descricao, String entidade, Long entidadeId, String detalhe) {

    /** Usado pela consulta JPQL; a descrição vem do enum. */
    public AuditoriaResposta(Long id, LocalDateTime dataHora, Long funcionarioId, String funcionario,
            AcaoAuditoria acao, String entidade, Long entidadeId, String detalhe) {
        this(id, dataHora, funcionarioId, funcionario, acao, acao.descricao(), entidade, entidadeId, detalhe);
    }
}
