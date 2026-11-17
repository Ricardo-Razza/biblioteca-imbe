package br.gov.imbe.biblioteca.acervo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Obra: os dados do ISBN. Cada cópia física é um exemplar. Nunca é apagada. */
@Entity
@Table(name = "obra")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "CHAR(13)")
    private String isbn13;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(length = 500)
    private String subtitulo;

    @Column(length = 255)
    private String editora;

    private Short ano;

    private Integer paginas;

    @Column(length = 10)
    private String idioma;

    @Column(columnDefinition = "TEXT")
    private String sinopse;

    @Column(length = 255)
    private String capaArquivo;

    @Column(length = 255)
    private String capaMiniatura;

    /** VARCHAR no banco: sem o JdbcTypeCode o Hibernate espera ENUM nativo do MySQL. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private FonteIsbn fonteDados;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<ObraAutor> autores = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "obra_assunto",
            joinColumns = @JoinColumn(name = "obra_id"),
            inverseJoinColumns = @JoinColumn(name = "assunto_id"))
    private Set<Assunto> assuntos = new LinkedHashSet<>();

    protected Obra() {
    }

    public Obra(String isbn13, String titulo) {
        this.isbn13 = isbn13;
        this.titulo = titulo;
        this.criadoEm = LocalDateTime.now();
    }

    /**
     * Troca a lista de autores mantendo as linhas que continuam (só muda a ordem). Remover e reinserir o mesmo par
     * na mesma transação quebraria a chave primária, porque o Hibernate faz os inserts antes dos deletes.
     * A obra precisa já ter id.
     */
    void definirAutores(List<Autor> novos) {
        Map<Long, ObraAutor> atuais = autores.stream()
                .collect(Collectors.toMap(oa -> oa.getAutor().getId(), Function.identity()));
        List<ObraAutor> resultado = new ArrayList<>();
        short ordem = 1;
        for (Autor autor : novos) {
            ObraAutor existente = atuais.remove(autor.getId());
            if (existente != null) {
                existente.setOrdem(ordem);
                resultado.add(existente);
            } else {
                resultado.add(new ObraAutor(this, autor, ordem));
            }
            ordem++;
        }
        autores.removeIf(oa -> atuais.containsKey(oa.getAutor().getId()));
        for (ObraAutor oa : resultado) {
            if (!autores.contains(oa)) {
                autores.add(oa);
            }
        }
        autores.sort(Comparator.comparingInt(ObraAutor::getOrdem));
    }

    /** Nomes dos arquivos dentro de {@code CAPAS_DIR}. */
    void definirCapa(String arquivo, String miniatura) {
        this.capaArquivo = arquivo;
        this.capaMiniatura = miniatura;
    }

    void definirAssuntos(Set<Assunto> novos) {
        assuntos.clear();
        assuntos.addAll(novos);
    }

    void preencher(String isbn13, String titulo, String subtitulo, String editora, Short ano, Integer paginas,
            String idioma, String sinopse, FonteIsbn fonteDados) {
        this.isbn13 = isbn13;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.editora = editora;
        this.ano = ano;
        this.paginas = paginas;
        this.idioma = idioma;
        this.sinopse = sinopse;
        this.fonteDados = fonteDados;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public String getEditora() {
        return editora;
    }

    public Short getAno() {
        return ano;
    }

    public Integer getPaginas() {
        return paginas;
    }

    public String getIdioma() {
        return idioma;
    }

    public String getSinopse() {
        return sinopse;
    }

    public String getCapaArquivo() {
        return capaArquivo;
    }

    public String getCapaMiniatura() {
        return capaMiniatura;
    }

    public FonteIsbn getFonteDados() {
        return fonteDados;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<ObraAutor> getAutores() {
        return List.copyOf(autores);
    }

    public Set<Assunto> getAssuntos() {
        return Set.copyOf(assuntos);
    }
}
