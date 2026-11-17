package br.gov.imbe.biblioteca.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.emprestimo.Emprestimo;
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
 * Reserva feita pelo site. Enquanto aguarda a retirada, o exemplar fica {@code RESERVADO}; na retirada vira
 * empréstimo com o mesmo exemplar. Nunca é apagada: o histórico fica aqui.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "CHAR(8)")
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id")
    private Exemplar exemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leitor_id")
    private Leitor leitor;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    private LocalDate prazoRetirada;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id")
    private Emprestimo emprestimo;

    private LocalDateTime finalizadaEm;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private CanceladaPor canceladaPor;

    /** Quem registrou a retirada ou o cancelamento na gestão. */
    private Long funcionarioId;

    protected Reserva() {
    }

    Reserva(String protocolo, Obra obra, Exemplar exemplar, Leitor leitor, LocalDateTime agora,
            LocalDate prazoRetirada) {
        this.protocolo = protocolo;
        this.obra = obra;
        this.exemplar = exemplar;
        this.leitor = leitor;
        this.criadaEm = agora;
        this.prazoRetirada = prazoRetirada;
        this.status = StatusReserva.AGUARDANDO_RETIRADA;
    }

    boolean aguardando() {
        return status == StatusReserva.AGUARDANDO_RETIRADA;
    }

    void retirar(Emprestimo emprestimo, Long funcionarioId, LocalDateTime agora) {
        finalizar(StatusReserva.RETIRADA, agora);
        this.emprestimo = emprestimo;
        this.funcionarioId = funcionarioId;
    }

    void cancelar(CanceladaPor por, Long funcionarioId, LocalDateTime agora) {
        finalizar(StatusReserva.CANCELADA, agora);
        this.canceladaPor = por;
        this.funcionarioId = funcionarioId;
    }

    void expirar(LocalDateTime agora) {
        finalizar(StatusReserva.EXPIRADA, agora);
    }

    private void finalizar(StatusReserva novo, LocalDateTime agora) {
        if (!aguardando()) {
            throw new IllegalStateException("Reserva " + protocolo + " já está " + status);
        }
        this.status = novo;
        this.finalizadaEm = agora;
    }

    public Long getId() {
        return id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public Obra getObra() {
        return obra;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDate getPrazoRetirada() {
        return prazoRetirada;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public Emprestimo getEmprestimo() {
        return emprestimo;
    }

    public LocalDateTime getFinalizadaEm() {
        return finalizadaEm;
    }

    public CanceladaPor getCanceladaPor() {
        return canceladaPor;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }
}
