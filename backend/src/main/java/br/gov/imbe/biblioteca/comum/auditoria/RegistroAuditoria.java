package br.gov.imbe.biblioteca.comum.auditoria;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma linha da auditoria. Só se inclui, nunca se altera. */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Long funcionarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40, updatable = false)
    private AcaoAuditoria acao;

    @Column(length = 30, updatable = false)
    private String entidade;

    @Column(updatable = false)
    private Long entidadeId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String detalhe;

    protected RegistroAuditoria() {
    }

    RegistroAuditoria(Long funcionarioId, AcaoAuditoria acao, String entidade, Long entidadeId, String detalhe,
            LocalDateTime dataHora) {
        this.funcionarioId = funcionarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhe = detalhe;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
