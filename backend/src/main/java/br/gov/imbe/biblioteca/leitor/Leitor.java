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

    /** Criado pelo site ao reservar, com os dados que o cidadão informou; a equipe confere na retirada. */
    @Column(nullable = false)
    private boolean cadastroPendente;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Leitor() {
    }

    Leitor(String cpf) {
        this.cpf = cpf;
        this.criadoEm = LocalDateTime.now();
    }

    /**
     * Pré-cadastro pelo site: reserva já, mas só leva livro depois que a equipe conferir o documento e completar o
     * cadastro (responsável, endereço).
     */
    public static Leitor preCadastro(String nome, String cpf, LocalDate dataNascimento, String telefone, String email) {
        Leitor l = new Leitor(cpf);
        l.preencher(nome, cpf, dataNascimento, telefone, email, null, null);
        l.cadastroPendente = true;
        return l;
    }

    /** A equipe salvou o cadastro: dados conferidos. */
    void conferirCadastro() {
        this.cadastroPendente = false;
    }

    public boolean isCadastroPendente() {
        return cadastroPendente;
    }

    /** Dígitos verificadores do CPF; rejeita sequências repetidas (111.111.111-11). */
    public static boolean cpfValido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) {
                soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
            }
            int dv = (soma * 10) % 11 % 10;
            if (dv != digitos.charAt(posicao) - '0') {
                return false;
            }
        }
        return true;
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
