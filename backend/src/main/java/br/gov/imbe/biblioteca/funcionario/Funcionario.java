package br.gov.imbe.biblioteca.funcionario;

import java.time.Duration;
import java.time.LocalDateTime;

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

/** Funcionário da biblioteca. Nunca é apagado (tem empréstimos e auditoria): desativa-se. */
@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 60, updatable = false)
    private String login;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_autenticacao", nullable = false, length = 10, updatable = false)
    private TipoAutenticacao tipo;

    @Column(length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean trocarSenha;

    @Column(nullable = false)
    private Short tentativasFalhas = 0;

    private LocalDateTime bloqueadoAte;

    private LocalDateTime ultimoAcesso;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Funcionario() {
    }

    Funcionario(String nome, String login, TipoAutenticacao tipo, Perfil perfil, LocalDateTime agora) {
        this.nome = nome;
        this.login = login;
        this.tipo = tipo;
        this.perfil = perfil;
        this.criadoEm = agora;
    }

    void alterar(String nome, Perfil perfil, boolean ativo) {
        this.nome = nome;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    /** Senha provisória (cadastro, redefinição): a troca fica obrigatória. */
    void definirSenhaProvisoria(String hash) {
        this.senhaHash = hash;
        this.trocarSenha = true;
        desbloquearLogin();
    }

    public void trocarSenha(String hash) {
        this.senhaHash = hash;
        this.trocarSenha = false;
    }

    public void registrarAcesso(LocalDateTime agora) {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.ultimoAcesso = agora;
    }

    /** Conta uma senha errada; ao chegar no limite, trava o login até {@code agora + bloqueio}. */
    public void registrarFalha(int limite, Duration bloqueio, LocalDateTime agora) {
        this.tentativasFalhas = (short) (tentativasFalhas + 1);
        if (tentativasFalhas >= limite) {
            this.bloqueadoAte = agora.plus(bloqueio);
            this.tentativasFalhas = 0;
        }
    }

    void desbloquearLogin() {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public boolean loginBloqueadoEm(LocalDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getLogin() {
        return login;
    }

    public TipoAutenticacao getTipo() {
        return tipo;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isTrocarSenha() {
        return trocarSenha;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
