package br.gov.imbe.biblioteca.leitor;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;

@Service
public class LeitorService {

    private final LeitorRepository leitores;
    private final Clock relogio;
    private final Auditoria auditoria;

    public LeitorService(LeitorRepository leitores, Clock relogio, Auditoria auditoria) {
        this.leitores = leitores;
        this.relogio = relogio;
        this.auditoria = auditoria;
    }

    @Transactional
    public LeitorResposta cadastrar(LeitorRequisicao req) {
        String cpf = LeitorRequisicao.soDigitos(req.cpf());
        if (leitores.existsByCpf(cpf)) {
            throw cpfJaCadastrado();
        }
        Leitor leitor = new Leitor(cpf);
        aplicar(leitor, cpf, req);
        return LeitorResposta.de(leitores.save(leitor), hoje());
    }

    @Transactional
    public LeitorResposta alterar(Long id, LeitorRequisicao req) {
        Leitor leitor = entidade(id);
        String cpf = LeitorRequisicao.soDigitos(req.cpf());
        if (leitores.existsByCpfAndIdNot(cpf, id)) {
            throw cpfJaCadastrado();
        }
        aplicar(leitor, cpf, req);
        if (req.ativo() != null && req.ativo() != leitor.isAtivo()) {
            if (!req.ativo() && leitores.contarEmprestimosEmAberto(id) > 0) {
                throw new RegraNegocioException("O leitor está com livros emprestados. Registre as devoluções antes de desativar.");
            }
            leitor.definirAtivo(req.ativo());
        }
        if (leitor.isCadastroPendente()) {
            // Salvar pela gestão é a conferência do pré-cadastro feito no site.
            leitor.conferirCadastro();
            auditoria.registrar(AcaoAuditoria.LEITOR_CADASTRO_CONFERIDO, "LEITOR", id, null);
        }
        return LeitorResposta.de(leitor, hoje());
    }

    /** Admin: tira a penalidade por atraso antes do prazo. */
    @Transactional
    public LeitorResposta desbloquear(Long id) {
        Leitor leitor = entidade(id);
        if (leitor.getBloqueadoAte() != null) {
            auditoria.registrar(AcaoAuditoria.LEITOR_DESBLOQUEADO, "LEITOR", id,
                    "Bloqueado até " + leitor.getBloqueadoAte());
        }
        leitor.desbloquear();
        return LeitorResposta.de(leitor, hoje());
    }

    @Transactional(readOnly = true)
    public LeitorResposta buscar(Long id) {
        return LeitorResposta.de(entidade(id), hoje());
    }

    /** Termo com 3 ou mais dígitos busca também pelo começo do CPF. */
    @Transactional(readOnly = true)
    public Pagina<LeitorResposta> listar(String termo, Pageable paginacao) {
        LocalDate hoje = hoje();
        String t = termo == null ? "" : termo.strip();
        if (t.isEmpty()) {
            return Pagina.de(leitores.findAll(paginacao), l -> LeitorResposta.de(l, hoje));
        }
        String padrao = "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String digitos = t.replaceAll("\\D", "");
        // Sem dígitos suficientes, um padrão que nenhum CPF casa
        String cpf = digitos.length() >= 3 ? digitos + "%" : "-";
        return Pagina.de(leitores.buscar(padrao, cpf, paginacao), l -> LeitorResposta.de(l, hoje));
    }

    /** Para os outros módulos (empréstimo, reserva). */
    public Leitor entidade(Long id) {
        return leitores.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Leitor " + id + " não encontrado."));
    }

    private void aplicar(Leitor leitor, String cpf, LeitorRequisicao req) {
        leitor.preencher(req.nome().strip(), cpf, req.dataNascimento(), texto(req.telefone()),
                texto(req.email()) == null ? null : texto(req.email()).toLowerCase(), texto(req.endereco()),
                texto(req.responsavel()));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private static ConflitoException cpfJaCadastrado() {
        return new ConflitoException("Já existe um leitor com este CPF.");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
