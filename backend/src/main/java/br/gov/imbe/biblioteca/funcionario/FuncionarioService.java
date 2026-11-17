package br.gov.imbe.biblioteca.funcionario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioAlteracao;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioRequisicao;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.FuncionarioResposta;
import br.gov.imbe.biblioteca.funcionario.FuncionarioDtos.SenhaProvisoriaResposta;

/** Cadastro de funcionários pelo administrador. */
@Service
public class FuncionarioService {

    static final String ENTIDADE = "FUNCIONARIO";

    private final FuncionarioRepository funcionarios;
    private final PasswordEncoder senhas;
    private final Auditoria auditoria;
    private final Clock relogio;

    public FuncionarioService(FuncionarioRepository funcionarios, PasswordEncoder senhas, Auditoria auditoria,
            Clock relogio) {
        this.funcionarios = funcionarios;
        this.senhas = senhas;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResposta> listar() {
        LocalDateTime agora = agora();
        return funcionarios.findAllByOrderByAtivoDescNomeAsc().stream()
                .map(f -> FuncionarioResposta.de(f, agora)).toList();
    }

    @Transactional
    public SenhaProvisoriaResposta cadastrar(FuncionarioRequisicao req) {
        String login = req.login().strip().toLowerCase(Locale.ROOT);
        if (funcionarios.existsByLogin(login)) {
            throw new ConflitoException("Já existe um funcionário com o login " + login + ".");
        }
        Funcionario f = new Funcionario(req.nome().strip(), login, req.tipo(), req.perfil(), agora());
        String provisoria = null;
        if (req.tipo() == TipoAutenticacao.LOCAL) {
            provisoria = Senhas.provisoria();
            f.definirSenhaProvisoria(senhas.encode(provisoria));
        }
        f = funcionarios.save(f);
        auditoria.registrar(AcaoAuditoria.FUNCIONARIO_CADASTRADO, ENTIDADE, f.getId(),
                login + " (" + req.tipo() + ", " + req.perfil() + ")");
        return new SenhaProvisoriaResposta(FuncionarioResposta.de(f, agora()), provisoria);
    }

    @Transactional
    public FuncionarioResposta alterar(Long id, FuncionarioAlteracao alt) {
        Funcionario f = entidade(id);
        boolean euMesmo = Objects.equals(UsuarioAtual.idOuNulo(), id);
        if (euMesmo && !alt.ativo()) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        if (euMesmo && alt.perfil() != f.getPerfil()) {
            throw new RegraNegocioException("Você não pode mudar o seu próprio perfil. Peça a outro administrador.");
        }
        boolean deixaDeSerAdmin = f.isAtivo() && f.getPerfil() == Perfil.ADMIN
                && (alt.perfil() != Perfil.ADMIN || !alt.ativo());
        if (deixaDeSerAdmin && funcionarios.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("É preciso ter pelo menos um administrador ativo.");
        }

        List<String> mudancas = new ArrayList<>();
        if (!f.getNome().equals(alt.nome().strip())) {
            mudancas.add("nome");
        }
        if (f.getPerfil() != alt.perfil()) {
            mudancas.add("perfil " + f.getPerfil() + " → " + alt.perfil());
        }
        if (f.isAtivo() != alt.ativo()) {
            mudancas.add(alt.ativo() ? "reativado" : "desativado");
        }
        f.alterar(alt.nome().strip(), alt.perfil(), alt.ativo());
        if (!mudancas.isEmpty()) {
            auditoria.registrar(AcaoAuditoria.FUNCIONARIO_ALTERADO, ENTIDADE, id,
                    f.getLogin() + ": " + String.join(", ", mudancas));
        }
        return FuncionarioResposta.de(f, agora());
    }

    /** Nova senha provisória para usuário LOCAL que esqueceu a senha. Também destrava o login. */
    @Transactional
    public SenhaProvisoriaResposta redefinirSenha(Long id) {
        Funcionario f = entidade(id);
        if (f.getTipo() != TipoAutenticacao.LOCAL) {
            throw new RegraNegocioException("A senha de rede é redefinida no sistema da Prefeitura.");
        }
        String provisoria = Senhas.provisoria();
        f.definirSenhaProvisoria(senhas.encode(provisoria));
        auditoria.registrar(AcaoAuditoria.SENHA_REDEFINIDA, ENTIDADE, id, f.getLogin());
        return new SenhaProvisoriaResposta(FuncionarioResposta.de(f, agora()), provisoria);
    }

    private Funcionario entidade(Long id) {
        return funcionarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário " + id + " não encontrado."));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
