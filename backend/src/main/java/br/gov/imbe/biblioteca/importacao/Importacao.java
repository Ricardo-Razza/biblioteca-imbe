package br.gov.imbe.biblioteca.importacao;

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

/** Uma planilha enviada. Os itens (linhas) ficam em {@link ItemImportacao}. */
@Entity
@Table(name = "importacao")
public class Importacao {

    public enum Status {
        PROCESSANDO,
        CONCLUIDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String arquivo;

    private Long funcionarioId;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status status;

    private LocalDateTime finalizadaEm;

    protected Importacao() {
    }

    Importacao(String arquivo, Long funcionarioId, LocalDateTime agora) {
        this.arquivo = arquivo;
        this.funcionarioId = funcionarioId;
        this.criadaEm = agora;
        this.status = Status.PROCESSANDO;
    }

    void concluir(LocalDateTime agora) {
        status = Status.CONCLUIDA;
        finalizadaEm = agora;
    }

    void reabrir() {
        status = Status.PROCESSANDO;
        finalizadaEm = null;
    }

    public Long getId() {
        return id;
    }

    public String getArquivo() {
        return arquivo;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getFinalizadaEm() {
        return finalizadaEm;
    }
}
