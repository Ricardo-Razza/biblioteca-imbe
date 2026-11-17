package br.gov.imbe.biblioteca.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.auth.AuthDtos.FuncionarioLogado;
import br.gov.imbe.biblioteca.auth.AuthDtos.LoginRequisicao;
import br.gov.imbe.biblioteca.auth.AuthDtos.LoginResposta;
import br.gov.imbe.biblioteca.auth.AuthDtos.TrocaSenhaRequisicao;
import br.gov.imbe.biblioteca.auth.Tokens.TokenEmitido;
import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.NaoAutenticadoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import br.gov.imbe.biblioteca.funcionario.Funcionario;
import br.gov.imbe.biblioteca.funcionario.FuncionarioRepository;
import br.gov.imbe.biblioteca.funcionario.Senhas;
import br.gov.imbe.biblioteca.funcionario.TipoAutenticacao;

/**
 * Login AD ou local. O login não roda numa transação só: a contagem de tentativas e a auditoria precisam ficar
 * gravadas mesmo quando o login é recusado.
 */
@Service
public class AuthService {

    static final String CREDENCIAIS_INVALIDAS = "Login ou senha incorretos.";
    private static final String ENTIDADE = "FUNCIONARIO";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final FuncionarioRepository funcionarios;
    private final PasswordEncoder senhas;
    private final ValidadorAd ad;
    private final Tokens tokens;
    private final Auditoria auditoria;
    private final AuthProperties propriedades;
    private final Clock relogio;
    /** Hash de referência: login inexistente gasta o mesmo tempo que senha errada. */
    private final String hashFalso;

    public AuthService(FuncionarioRepository funcionarios, PasswordEncoder senhas, ValidadorAd ad, Tokens tokens,
            Auditoria auditoria, AuthProperties propriedades, Clock relogio) {
        this.funcionarios = funcionarios;
        this.senhas = senhas;
        this.ad = ad;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.propriedades = propriedades;
        this.relogio = relogio;
        this.hashFalso = senhas.encode("senha-que-ninguem-usa");
    }

    public static String normalizarLogin(String login) {
        return login == null ? "" : login.strip().toLowerCase(Locale.ROOT);
    }

    public LoginResposta login(LoginRequisicao req) {
        String login = normalizarLogin(req.login());
        LocalDateTime agora = LocalDateTime.now(relogio);
        Funcionario f = funcionarios.findByLogin(login).orElse(null);

        if (f == null || !f.isAtivo()) {
            senhas.matches(req.senha(), hashFalso);
            auditoria.registrarPor(null, AcaoAuditoria.LOGIN_RECUSADO, ENTIDADE, f == null ? null : f.getId(),
                    "Login \"" + login + "\" " + (f == null ? "não cadastrado" : "desativado"));
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        if (f.loginBloqueadoEm(agora)) {
            throw travado(f);
        }

        boolean confere = f.getTipo() == TipoAutenticacao.LOCAL
                ? senhas.matches(req.senha(), f.getSenhaHash())
                : ad.validar(login, req.senha());

        if (!confere) {
            f.registrarFalha(propriedades.tentativas(), propriedades.bloqueio(), agora);
            funcionarios.save(f);
            boolean travou = f.loginBloqueadoEm(agora);
            auditoria.registrarPor(f.getId(), travou ? AcaoAuditoria.LOGIN_TRAVADO : AcaoAuditoria.LOGIN_RECUSADO,
                    ENTIDADE, f.getId(), "Senha incorreta");
            if (travou) {
                throw travado(f);
            }
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }

        f.registrarAcesso(agora);
        f = funcionarios.save(f);
        auditoria.registrarPor(f.getId(), AcaoAuditoria.LOGIN, ENTIDADE, f.getId(), f.getTipo().name());
        TokenEmitido token = tokens.emitir(f);
        return new LoginResposta(token.token(), token.expiraEm(), FuncionarioLogado.de(f));
    }

    @Transactional(readOnly = true)
    public FuncionarioLogado eu() {
        return FuncionarioLogado.de(atual());
    }

    @Transactional
    public FuncionarioLogado trocarSenha(TrocaSenhaRequisicao req) {
        Funcionario f = atual();
        if (f.getTipo() != TipoAutenticacao.LOCAL) {
            throw new RegraNegocioException("A senha de rede é trocada no sistema da Prefeitura.");
        }
        if (!senhas.matches(req.senhaAtual(), f.getSenhaHash())) {
            throw new RequisicaoInvalidaException("Senha atual incorreta", "A senha atual não confere.");
        }
        if (req.novaSenha().equals(req.senhaAtual())) {
            throw new RequisicaoInvalidaException("Senha repetida", "A nova senha precisa ser diferente da atual.");
        }
        Senhas.validarNova(req.novaSenha(), f.getLogin());
        f.trocarSenha(senhas.encode(req.novaSenha()));
        auditoria.registrar(AcaoAuditoria.SENHA_TROCADA, ENTIDADE, f.getId(), null);
        return FuncionarioLogado.de(f);
    }

    private Funcionario atual() {
        return UsuarioAtual.id().flatMap(funcionarios::findById)
                .orElseThrow(() -> new NaoAutenticadoException("Entre de novo no sistema."));
    }

    private static NaoAutenticadoException travado(Funcionario f) {
        return new NaoAutenticadoException("Muitas tentativas erradas. Tente de novo a partir das "
                + HORA.format(f.getBloqueadoAte()) + ".");
    }
}
