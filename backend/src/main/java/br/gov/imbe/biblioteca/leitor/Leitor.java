package br.gov.imbe.biblioteca.leitor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Leitor da biblioteca. Nunca é apagado: desativa-se. */
@Entity
@Table(name = "leitor")
public class Leitor {

    public static final int MAIORIDADE = 18;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, columnDefinition = "CHAR(11)")
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String endereco;

    @Column(length = 200)
    private String responsavel;

    @Column(nullable = false)
    private boolean ativo = true;

    private LocalDate bloqueadoAte;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Leitor() {
    }

    Leitor(String cpf) {
        this.cpf = cpf;
        this.criadoEm = LocalDateTime.now();
    }

    void preencher(String nome, String cpf, LocalDate dataNascimento, String telefone, String email, String endereco,
            String responsavel) {
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
        this.email = email;
        this.endereco = endereco;
        this.responsavel = responsavel;
    }

    void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /** Penalidade por atraso: estende o bloqueio, nunca encurta. */
    public void bloquearAte(LocalDate data) {
        if (bloqueadoAte == null || data.isAfter(bloqueadoAte)) {
            bloqueadoAte = data;
        }
    }

    /** O administrador pode liberar antes do prazo. */
    void desbloquear() {
        bloqueadoAte = null;
    }

    /** Bloqueado até a data inclusive. */
    public boolean bloqueadoEm(LocalDate hoje) {
        return bloqueadoAte != null && !hoje.isAfter(bloqueadoAte);
    }

    public boolean menorDeIdadeEm(LocalDate hoje) {
        return Period.between(dataNascimento, hoje).getYears() < MAIORIDADE;
    }

    /** "Maria" de "Maria da Silva": o site público só mostra o primeiro nome. */
    public String primeiroNome() {
        return nome.strip().split("\\s+")[0];
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDate getBloqueadoAte() {
        return bloqueadoAte;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
