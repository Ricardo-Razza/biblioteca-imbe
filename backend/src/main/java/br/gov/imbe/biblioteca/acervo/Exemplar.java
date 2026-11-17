package br.gov.imbe.biblioteca.acervo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
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

/** Cópia física de uma obra, identificada pelo tombo. O empréstimo aponta para o exemplar. Nunca é apagado. */
@Entity
@Table(name = "exemplar")
public class Exemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Column(nullable = false, length = 30, updatable = false)
    private String tombo;

    @Column(length = 100)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoExemplar estado = EstadoExemplar.BOM;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoExemplar situacao = SituacaoExemplar.DISPONIVEL;

    private LocalDate dataAquisicao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OrigemExemplar origem;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Exemplar() {
    }

    Exemplar(Obra obra, String tombo, String localizacao, EstadoExemplar estado, LocalDate dataAquisicao,
            OrigemExemplar origem) {
        this.obra = obra;
        this.tombo = tombo;
        this.localizacao = localizacao;
        this.estado = estado == null ? EstadoExemplar.BOM : estado;
        this.dataAquisicao = dataAquisicao;
        this.origem = origem;
        this.criadoEm = LocalDateTime.now();
    }

    /**
     * Mudança manual de situação (gestão). Reserva e empréstimo mudam a situação pelos próprios fluxos.
     * Regras: baixado é definitivo; emprestado ou reservado só muda pelo fluxo dele; manualmente só se vai para
     * extraviado, baixado ou, de extraviado, de volta a disponível.
     */
    void mudarSituacao(SituacaoExemplar nova) {
        if (nova == situacao) {
            return;
        }
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("Exemplar " + tombo + " está baixado e não volta ao acervo.");
        }
        if (situacao == SituacaoExemplar.EMPRESTADO || situacao == SituacaoExemplar.RESERVADO) {
            throw new RegraNegocioException("Exemplar " + tombo + " está " + descricao(situacao)
                    + ". Registre a devolução ou cancele a reserva antes.");
        }
        boolean permitida = switch (nova) {
            case EXTRAVIADO, BAIXADO -> true;
            case DISPONIVEL -> situacao == SituacaoExemplar.EXTRAVIADO;
            case EMPRESTADO, RESERVADO -> false;
        };
        if (!permitida) {
            throw new RegraNegocioException("Não é possível marcar o exemplar como " + descricao(nova)
                    + " por aqui. Use o empréstimo ou a reserva.");
        }
        situacao = nova;
    }

    void alterar(String localizacao, EstadoExemplar estado) {
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("Exemplar " + tombo + " está baixado e não pode ser alterado.");
        }
        this.localizacao = localizacao;
        if (estado != null) {
            this.estado = estado;
        }
    }

    private static String descricao(SituacaoExemplar s) {
        return switch (s) {
            case DISPONIVEL -> "disponível";
            case RESERVADO -> "reservado";
            case EMPRESTADO -> "emprestado";
            case EXTRAVIADO -> "extraviado";
            case BAIXADO -> "baixado";
        };
    }

    public Long getId() {
        return id;
    }

    public Obra getObra() {
        return obra;
    }

    public String getTombo() {
        return tombo;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public EstadoExemplar getEstado() {
        return estado;
    }

    public SituacaoExemplar getSituacao() {
        return situacao;
    }

    public LocalDate getDataAquisicao() {
        return dataAquisicao;
    }

    public OrigemExemplar getOrigem() {
        return origem;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
