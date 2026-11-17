package br.gov.imbe.biblioteca.comum.calendario;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "feriado")
public class Feriado {

    @Id
    private LocalDate data;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoFeriado tipo;

    protected Feriado() {
    }

    Feriado(LocalDate data, String descricao, TipoFeriado tipo) {
        this.data = data;
        this.descricao = descricao;
        this.tipo = tipo;
    }

    void alterar(String descricao, TipoFeriado tipo) {
        this.descricao = descricao;
        this.tipo = tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoFeriado getTipo() {
        return tipo;
    }
}
