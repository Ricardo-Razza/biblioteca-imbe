package br.gov.imbe.biblioteca.acervo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Autor, compartilhado entre obras. O nome é único sem diferenciar maiúsculas nem acentos
 * (collation utf8mb4_0900_ai_ci): "JOSÉ SARAMAGO" reaproveita "José Saramago".
 */
@Entity
@Table(name = "autor")
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    protected Autor() {
    }

    public Autor(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
