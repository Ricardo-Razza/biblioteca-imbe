package br.gov.imbe.biblioteca.acervo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** Autor de uma obra, com a ordem em que aparece na capa (1 = principal). */
@Entity
@Table(name = "obra_autor")
public class ObraAutor {

    @EmbeddedId
    private Id id;

    @MapsId("obraId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @MapsId("autorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Autor autor;

    @Column(nullable = false)
    private short ordem;

    protected ObraAutor() {
    }

    ObraAutor(Obra obra, Autor autor, short ordem) {
        this.id = new Id(obra.getId(), autor.getId());
        this.obra = obra;
        this.autor = autor;
        this.ordem = ordem;
    }

    public Autor getAutor() {
        return autor;
    }

    public short getOrdem() {
        return ordem;
    }

    void setOrdem(short ordem) {
        this.ordem = ordem;
    }

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "obra_id")
        private Long obraId;

        @Column(name = "autor_id")
        private Long autorId;

        protected Id() {
        }

        Id(Long obraId, Long autorId) {
            this.obraId = obraId;
            this.autorId = autorId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Id outro && Objects.equals(obraId, outro.obraId) && Objects.equals(autorId, outro.autorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(obraId, autorId);
        }
    }
}
