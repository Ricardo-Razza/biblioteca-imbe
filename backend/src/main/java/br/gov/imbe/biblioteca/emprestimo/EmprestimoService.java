package br.gov.imbe.biblioteca.emprestimo;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.acervo.ExemplarRepository;
import br.gov.imbe.biblioteca.acervo.ExemplarService;
import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.calendario.Calendario;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
import br.gov.imbe.biblioteca.comum.parametro.ChaveParametro;
import br.gov.imbe.biblioteca.comum.parametro.ParametroService;
import br.gov.imbe.biblioteca.leitor.Leitor;
import br.gov.imbe.biblioteca.leitor.LeitorService;

/**
 * Balcão: empréstimo, renovação e devolução, com as regras do README (limite, atraso, bloqueio).
 * Prazos que caem em fim de semana ou feriado passam para o próximo dia útil.
 */
@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimos;
    private final ExemplarRepository exemplares;
    private final LeitorService leitores;
    private final ParametroService parametros;
    private final Calendario calendario;
    private final Optional<ReservasAtivas> reservas;
    private final Clock relogio;

    public EmprestimoService(EmprestimoRepository emprestimos, ExemplarRepository exemplares, LeitorService leitores,
            ParametroService parametros, Calendario calendario, Optional<ReservasAtivas> reservas, Clock relogio) {
        this.emprestimos = emprestimos;
        this.exemplares = exemplares;
        this.leitores = leitores;
        this.parametros = parametros;
        this.calendario = calendario;
        this.reservas = reservas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public SituacaoLeitor situacao(Long leitorId) {
        return situacao(leitores.entidade(leitorId), hoje());
    }

    @Transactional
    public EmprestimoResposta emprestar(EmprestimoRequisicao req, Long funcionarioId) {
        LocalDate hoje = hoje();
        Leitor leitor = leitores.entidade(req.leitorId());
        SituacaoLeitor situacao = situacao(leitor, hoje);
        if (!situacao.podeEmprestar()) {
            throw new RegraNegocioException(situacao.motivos().getFirst());
        }
        String tombo = ExemplarService.normalizarTombo(req.tombo());
        Exemplar exemplar = exemplares.travarPorTombo(tombo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum exemplar com o tombo " + tombo + "."));
        exemplar.emprestar();
        return EmprestimoResposta.de(abrir(exemplar, leitor, hoje, funcionarioId), hoje, maximoRenovacoes());
    }

    /**
     * Abre o empréstimo de um exemplar já liberado para isso (disponível ou retirado de uma reserva).
     * Usado também pela retirada de reserva.
     */
    @Transactional
    public Emprestimo abrir(Exemplar exemplar, Leitor leitor, LocalDate hoje, Long funcionarioId) {
        LocalDate prevista = calendario.proximoDiaUtil(hoje.plusDays(parametros.inteiro(ChaveParametro.PRAZO_EMPRESTIMO_DIAS)));
        return emprestimos.save(new Emprestimo(exemplar, leitor, hoje, prevista, funcionarioId));
    }

    @Transactional
    public EmprestimoResposta renovar(Long id) {
        LocalDate hoje = hoje();
        Emprestimo e = entidade(id);
        if (!e.emAberto()) {
            throw new RegraNegocioException("Este empréstimo já foi devolvido.");
        }
        if (e.diasAtraso(hoje) > 0) {
            throw new RegraNegocioException("O empréstimo está atrasado. Registre a devolução; não dá para renovar.");
        }
        Leitor leitor = e.getLeitor();
        if (leitor.bloqueadoEm(hoje)) {
            throw new RegraNegocioException("Leitor bloqueado até " + data(leitor.getBloqueadoAte()) + " por atraso.");
        }
        if (emprestimos.contarAtrasados(leitor.getId(), hoje) > 0) {
            throw new RegraNegocioException("O leitor tem livro em atraso. Registre a devolução antes de renovar.");
        }
        int maximo = maximoRenovacoes();
        if (e.getRenovacoes() >= maximo) {
            throw new RegraNegocioException("Este empréstimo já foi renovado " + maximo
                    + (maximo == 1 ? " vez" : " vezes") + ", o máximo permitido.");
        }
        e.renovar(calendario.proximoDiaUtil(hoje.plusDays(parametros.inteiro(ChaveParametro.DIAS_POR_RENOVACAO))));
        return EmprestimoResposta.de(e, hoje, maximo);
    }

    /**
     * Devolução. Com atraso, o leitor fica bloqueado pelo mesmo número de dias, contando o dia da devolução
     * (3 dias de atraso: bloqueado hoje, amanhã e depois).
     */
    @Transactional
    public EmprestimoResposta devolver(Long id, DevolucaoRequisicao req, Long funcionarioId) {
        LocalDate hoje = hoje();
        Emprestimo e = entidade(id);
        if (!e.emAberto()) {
            throw new RegraNegocioException("Este empréstimo já foi devolvido em " + data(e.getDataDevolucao()) + ".");
        }
        OcorrenciaDevolucao ocorrencia = req == null ? null : req.ocorrencia();
        String observacao = req == null || req.observacao() == null || req.observacao().isBlank() ? null
                : req.observacao().strip();
        Exemplar exemplar = exemplares.travar(e.getExemplar().getId()).orElseThrow();
        exemplar.devolver(ocorrencia == OcorrenciaDevolucao.DANIFICADO, ocorrencia == OcorrenciaDevolucao.EXTRAVIADO);
        long atraso = e.diasAtraso(hoje);
        e.devolver(hoje, ocorrencia, observacao, funcionarioId);
        LocalDate bloqueio = null;
        if (atraso > 0) {
            bloqueio = hoje.plusDays(atraso - 1);
            e.getLeitor().bloquearAte(bloqueio);
            bloqueio = e.getLeitor().getBloqueadoAte();
        }
        return EmprestimoResposta.de(e, hoje, maximoRenovacoes(), bloqueio);
    }

    @Transactional(readOnly = true)
    public EmprestimoResposta buscar(Long id) {
        return EmprestimoResposta.de(entidade(id), hoje(), maximoRenovacoes());
    }

    /**
     * Lista com filtros. Em aberto e atrasados saem por data prevista (o que vence antes primeiro);
     * devolvidos, do mais recente para o mais antigo.
     */
    @Transactional(readOnly = true)
    public Pagina<EmprestimoResposta> listar(StatusEmprestimo status, Long leitorId, String tombo, String termo,
            int pagina, int tamanho) {
        LocalDate hoje = hoje();
        StatusEmprestimo s = status == null ? StatusEmprestimo.ATIVO : status;
        int codigo = switch (s) {
            case ATIVO -> 0;
            case ATRASADO -> 1;
            case DEVOLVIDO -> 2;
        };
        Sort ordem = s == StatusEmprestimo.DEVOLVIDO
                ? Sort.by(Sort.Order.desc("dataDevolucao"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.asc("dataPrevista"), Sort.Order.asc("id"));
        Pageable p = PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanho)), ordem);
        String t = termo == null || termo.isBlank() ? null : termo.strip();
        String padrao = t == null ? null : "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String tomboNormal = tombo == null || tombo.isBlank() ? null : ExemplarService.normalizarTombo(tombo);
        int maximo = maximoRenovacoes();
        return Pagina.de(emprestimos.buscar(codigo, hoje, leitorId, tomboNormal, padrao, p),
                e -> EmprestimoResposta.de(e, hoje, maximo));
    }

    SituacaoLeitor situacao(Leitor leitor, LocalDate hoje) {
        long abertos = emprestimos.contarEmAberto(leitor.getId());
        long atrasados = emprestimos.contarAtrasados(leitor.getId(), hoje);
        long reservados = reservas.map(r -> (long) r.contar(leitor.getId())).orElse(0L);
        int limite = parametros.inteiro(ChaveParametro.LIMITE_LIVROS_POR_LEITOR);
        List<String> motivos = new ArrayList<>();
        if (!leitor.isAtivo()) {
            motivos.add("Leitor desativado.");
        }
        if (leitor.bloqueadoEm(hoje)) {
            motivos.add("Leitor bloqueado até " + data(leitor.getBloqueadoAte()) + " por atraso.");
        }
        if (atrasados > 0) {
            motivos.add(atrasados == 1 ? "Tem 1 livro em atraso." : "Tem " + atrasados + " livros em atraso.");
        }
        if (abertos + reservados >= limite) {
            motivos.add("Já está com " + (abertos + reservados) + " de " + limite + " livros permitidos"
                    + (reservados > 0 ? " (contando as reservas)." : "."));
        }
        if (leitor.menorDeIdadeEm(hoje) && leitor.getResponsavel() == null) {
            motivos.add("Leitor menor de idade sem responsável. Complete o cadastro.");
        }
        return new SituacaoLeitor(leitor.getId(), leitor.getNome(), leitor.isAtivo(),
                leitor.bloqueadoEm(hoje) ? leitor.getBloqueadoAte() : null, abertos, atrasados, reservados,
                abertos + reservados, limite, motivos.isEmpty(), List.copyOf(motivos));
    }

    private Emprestimo entidade(Long id) {
        return emprestimos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo " + id + " não encontrado."));
    }

    private int maximoRenovacoes() {
        return parametros.inteiro(ChaveParametro.MAXIMO_RENOVACOES);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private static String data(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
