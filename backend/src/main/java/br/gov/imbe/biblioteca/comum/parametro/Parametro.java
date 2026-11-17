package br.gov.imbe.biblioteca.comum.parametro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Regra configurável pelo administrador (prazos e limites). As chaves estão em {@link ChaveParametro}. */
@Entity
@Table(name = "parametro")
public class Parametro {

    @Id
    @Column(length = 50)
    private String chave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(nullable = false, length = 255)
    private String descricao;

    protected Parametro() {
    }

    public String getChave() {
        return chave;
    }

    public String getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    void alterar(String valor) {
        this.valor = valor;
    }
}
