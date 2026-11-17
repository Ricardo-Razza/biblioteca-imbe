package br.gov.imbe.biblioteca.emprestimo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.leitor.Leitor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Empréstimo de um exemplar. Em aberto enquanto {@code dataDevolucao} é nula; atrasado quando, em aberto,
 * a data prevista já passou. Não há tabela de histórico: o histórico é esta tabela.
 */
@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id")
    private Exemplar exemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leitor_id")
    private Leitor leitor;

    private Long funcionarioSaidaId;

    @Column(nullable = false)
    private LocalDate dataSaida;

    @Column(nullable = false)
    private LocalDate dataPrevista;

    private LocalDate dataDevolucao;

    private Long funcionarioDevolucaoId;

    @Column(nullable = false)
    private short renovacoes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OcorrenciaDevolucao ocorrenciaDevolucao;

    @Column(length = 500)
    private String observacao;

    protected Emprestimo() {
    }

    Emprestimo(Exemplar exemplar, Leitor leitor, LocalDate hoje, LocalDate prevista, Long funcionarioId) {
        this.exemplar = exemplar;
        this.leitor = leitor;
        this.dataSaida = hoje;
        this.dataPrevista = prevista;
        this.funcionarioSaidaId = funcionarioId;
    }

    public boolean emAberto() {
        return dataDevolucao == null;
    }

    /** Dias corridos de atraso: em aberto, até hoje; devolvido, até a devolução. */
    public long diasAtraso(LocalDate hoje) {
        LocalDate fim = emAberto() ? hoje : dataDevolucao;
        return Math.max(0, ChronoUnit.DAYS.between(dataPrevista, fim));
    }

    public StatusEmprestimo status(LocalDate hoje) {
        if (!emAberto()) {
            return StatusEmprestimo.DEVOLVIDO;
        }
        return diasAtraso(hoje) > 0 ? StatusEmprestimo.ATRASADO : StatusEmprestimo.ATIVO;
    }

    void renovar(LocalDate novaPrevista) {
        renovacoes++;
        if (novaPrevista.isAfter(dataPrevista)) {
            dataPrevista = novaPrevista;
        }
    }

    void devolver(LocalDate hoje, OcorrenciaDevolucao ocorrencia, String observacao, Long funcionarioId) {
        this.dataDevolucao = hoje;
        this.ocorrenciaDevolucao = ocorrencia;
        this.observacao = observacao;
        this.funcionarioDevolucaoId = funcionarioId;
    }

    public Long getId() {
        return id;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public Long getFuncionarioSaidaId() {
        return funcionarioSaidaId;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public Long getFuncionarioDevolucaoId() {
        return funcionarioDevolucaoId;
    }

    public int getRenovacoes() {
        return renovacoes;
    }

    public OcorrenciaDevolucao getOcorrenciaDevolucao() {
        return ocorrenciaDevolucao;
    }

    public String getObservacao() {
        return observacao;
    }
}
