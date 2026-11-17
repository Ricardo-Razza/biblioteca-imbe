package br.gov.imbe.biblioteca.importacao;

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

/** Uma linha da planilha e o que aconteceu com ela. */
@Entity
@Table(name = "importacao_item")
public class ItemImportacao {

    static final int TAMANHO_MENSAGEM = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long importacaoId;

    @Column(nullable = false)
    private int linha;

    @Column(nullable = false, length = 40)
    private String isbnOriginal;

    @Column(columnDefinition = "CHAR(13)")
    private String isbn13;

    @Column(length = 30)
    private String tombo;

    @Column(length = 100)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ResultadoItem resultado;

    private Long obraId;

    @Column(length = TAMANHO_MENSAGEM)
    private String mensagem;

    protected ItemImportacao() {
    }

    ItemImportacao(Long importacaoId, LinhaPlanilha l) {
        this.importacaoId = importacaoId;
        this.linha = l.numero();
        this.isbnOriginal = l.isbnOriginal();
        this.isbn13 = l.isbn13();
        this.tombo = l.tombo();
        this.localizacao = l.localizacao();
        if (l.erro() != null) {
            this.resultado = ResultadoItem.ERRO;
            this.mensagem = l.erro();
        } else {
            this.resultado = ResultadoItem.PENDENTE;
        }
    }

    void registrar(ResultadoItem resultado, Long obraId, String mensagem) {
        this.resultado = resultado;
        this.obraId = obraId;
        this.mensagem = mensagem == null || mensagem.length() <= TAMANHO_MENSAGEM ? mensagem
                : mensagem.substring(0, TAMANHO_MENSAGEM);
    }

    void voltarParaFila() {
        this.resultado = ResultadoItem.PENDENTE;
        this.mensagem = null;
    }

    public Long getId() {
        return id;
    }

    public Long getImportacaoId() {
        return importacaoId;
    }

    public int getLinha() {
        return linha;
    }

    public String getIsbnOriginal() {
        return isbnOriginal;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getTombo() {
        return tombo;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public ResultadoItem getResultado() {
        return resultado;
    }

    public Long getObraId() {
        return obraId;
    }

    public String getMensagem() {
        return mensagem;
    }
}
