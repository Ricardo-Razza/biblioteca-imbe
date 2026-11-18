package br.gov.imbe.biblioteca.reserva;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.acervo.ExemplarRepository;
import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.acervo.ObraRepository;
import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.calendario.Calendario;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;
import br.gov.imbe.biblioteca.comum.parametro.ChaveParametro;
import br.gov.imbe.biblioteca.comum.parametro.ParametroService;
import br.gov.imbe.biblioteca.emprestimo.Emprestimo;
import br.gov.imbe.biblioteca.emprestimo.EmprestimoService;
import br.gov.imbe.biblioteca.emprestimo.ReservasAtivas;
import br.gov.imbe.biblioteca.emprestimo.SituacaoLeitor;
import br.gov.imbe.biblioteca.leitor.Leitor;
import br.gov.imbe.biblioteca.leitor.LeitorRepository;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.MinhasReservas;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.ReservaPublica;

/**
 * Reservas: pelo site (cidadão identificado por CPF e data de nascimento) e na gestão (entrega e cancelamento).
 * Travas sempre na ordem exemplar → reserva, inclusive no balcão, para não haver impasse entre fluxos.
 */
@Service
public class ReservaService implements ReservasAtivas {

    /** Mensagem única para CPF inexistente, data errada ou CPF malformado: não revela quem é leitor. */
    public static final String NAO_CONFERE = "CPF ou data de nascimento não conferem.";

    private static final String ENTIDADE = "RESERVA";
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_PROTOCOLO = 8;
    private static final SecureRandom SORTEIO = new SecureRandom();

    private final ReservaRepository reservas;
    private final ExemplarRepository exemplares;
    private final ObraRepository obras;
    private final LeitorRepository leitores;
    private final EmprestimoService emprestimos;
    private final ParametroService parametros;
    private final Calendario calendario;
    private final Auditoria auditoria;
    private final Clock relogio;

    public ReservaService(ReservaRepository reservas, ExemplarRepository exemplares, ObraRepository obras,
            LeitorRepository leitores, EmprestimoService emprestimos, ParametroService parametros,
            Calendario calendario, Auditoria auditoria, Clock relogio) {
        this.reservas = reservas;
        this.exemplares = exemplares;
        this.obras = obras;
        this.leitores = leitores;
        this.emprestimos = emprestimos;
        this.parametros = parametros;
        this.calendario = calendario;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- balcão

    @Override
    @Transactional(readOnly = true)
    public int contar(Long leitorId) {
        return (int) reservas.countByLeitorIdAndStatus(leitorId, StatusReserva.AGUARDANDO_RETIRADA);
    }

    @Override
    @Transactional
    public Optional<Emprestimo> retirarNoBalcao(Exemplar exemplar, Leitor leitor, Long funcionarioId) {
        return reservas.travarAguardandoDoExemplar(exemplar.getId())
                .filter(r -> r.getLeitor().getId().equals(leitor.getId()))
                .map(r -> retirar(r, exemplar, funcionarioId));
    }

    // ---------------------------------------------------------------- site público

    /**
     * Reserva pelo site. Confere o leitor, os limites e a disponibilidade e prende um exemplar na mesma transação.
     * O prazo conta em dias úteis a partir de hoje.
     */
    @Transactional
    public ReservaPublica reservar(Long obraId, String nome, String cpf, LocalDate dataNascimento, String telefone,
            String email) {
        String telefoneContato = telefoneComDdd(telefone);
        String emailContato = email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
        Leitor leitor = identificarOuPreCadastrar(nome, cpf, dataNascimento, telefoneContato, emailContato);
        Obra obra = obras.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado."));
        exigirPodeReservar(leitor);
        reservas.findFirstByLeitorIdAndObraIdAndStatus(leitor.getId(), obraId, StatusReserva.AGUARDANDO_RETIRADA)
                .ifPresent(r -> {
                    throw new ConflitoException("Você já tem uma reserva deste livro, com o protocolo "
                            + r.getProtocolo() + ".");
                });
        Exemplar exemplar = exemplares.travarDisponivelDaObra(obraId)
                .orElseThrow(() -> new RegraNegocioException(
                        "Não há exemplar disponível deste livro agora. Todos estão emprestados ou reservados."));
        exemplar.reservar();
        LocalDate prazo = calendario.somarDiasUteis(hoje(),
                parametros.inteiro(ChaveParametro.PRAZO_RETIRADA_RESERVA_DIAS_UTEIS));
        Reserva r = reservas.save(new Reserva(novoProtocolo(), obra, exemplar, leitor, agora(), prazo,
                telefoneContato, emailContato));
        return ReservaPublica.de(r);
    }

    /**
     * Telefone brasileiro com DDD (fixo ou celular), aceitando +55 na frente. Gravado como "(51) 99999-9999".
     */
    static String telefoneComDdd(String telefone) {
        String d = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if ((d.length() == 12 || d.length() == 13) && d.startsWith("55")) {
            d = d.substring(2);
        }
        if (d.length() != 10 && d.length() != 11) {
            throw new RequisicaoInvalidaException("Telefone inválido",
                    "Telefone inválido. Use o DDD, ex.: (51) 99999-9999.");
        }
        int corte = d.length() - 4;
        return "(" + d.substring(0, 2) + ") " + d.substring(2, corte) + "-" + d.substring(corte);
    }

    @Transactional(readOnly = true)
    public MinhasReservas consultar(String cpf, LocalDate dataNascimento) {
        Leitor leitor = identificar(cpf, dataNascimento);
        return new MinhasReservas(leitor.primeiroNome(),
                reservas.findTop20ByLeitorIdOrderByIdDesc(leitor.getId()).stream().map(ReservaPublica::de).toList());
    }

    /** Cancelamento pelo próprio leitor. Protocolo de outro leitor responde como inexistente. */
    @Transactional
    public ReservaPublica cancelarPeloLeitor(String protocolo, String cpf, LocalDate dataNascimento) {
        Leitor leitor = identificar(cpf, dataNascimento);
        String p = normalizarProtocolo(protocolo);
        Long exemplarId = reservas.exemplarDoProtocolo(p).orElseThrow(ReservaService::protocoloNaoEncontrado);
        Exemplar exemplar = exemplares.travar(exemplarId).orElseThrow();
        Reserva r = reservas.travarPorProtocolo(p).orElseThrow(ReservaService::protocoloNaoEncontrado);
        if (!r.getLeitor().getId().equals(leitor.getId())) {
            throw protocoloNaoEncontrado();
        }
        exigirAguardando(r);
        r.cancelar(CanceladaPor.LEITOR, null, agora());
        exemplar.liberarReserva();
        return ReservaPublica.de(r);
    }

    // ---------------------------------------------------------------- gestão

    /**
     * {@code status} vazio: aguardando retirada, por prazo (o que vence antes primeiro). Finalizadas saem da mais
     * recente para a mais antiga. {@code termo}: título, nome do leitor, protocolo ou tombo.
     */
    @Transactional(readOnly = true)
    public Pagina<ReservaResposta> listar(Collection<StatusReserva> status, Long leitorId, String termo, int pagina,
            int tamanho) {
        Collection<StatusReserva> s = status == null || status.isEmpty()
                ? List.of(StatusReserva.AGUARDANDO_RETIRADA) : status;
        Sort ordem = s.size() == 1 && s.contains(StatusReserva.AGUARDANDO_RETIRADA)
                ? Sort.by(Sort.Order.asc("prazoRetirada"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("finalizadaEm"), Sort.Order.desc("id"));
        Pageable p = PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanho)), ordem);
        String t = termo == null || termo.isBlank() ? null : termo.strip();
        String padrao = t == null ? null : "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String protocolo = t == null ? null : normalizarProtocolo(t);
        String tombo = t == null ? null : t.toUpperCase(Locale.ROOT);
        LocalDate hoje = hoje();
        return Pagina.de(reservas.buscar(s, leitorId, padrao, protocolo, tombo, p), r -> ReservaResposta.de(r, hoje));
    }

    @Transactional(readOnly = true)
    public ReservaResposta buscar(Long id) {
        return ReservaResposta.de(reservas.findById(id).orElseThrow(() -> naoEncontrada(id)), hoje());
    }

    /**
     * Entrega no balcão: a reserva vira empréstimo com o mesmo exemplar. Vale também com o prazo vencido, enquanto
     * o job não expirou a reserva (o livro continua separado).
     */
    @Transactional
    public ReservaResposta retirar(Long id, Long funcionarioId) {
        Long exemplarId = reservas.exemplarDa(id).orElseThrow(() -> naoEncontrada(id));
        Exemplar exemplar = exemplares.travar(exemplarId).orElseThrow();
        Reserva r = reservas.travar(id).orElseThrow(() -> naoEncontrada(id));
        exigirAguardando(r);
        retirar(r, exemplar, funcionarioId);
        return ReservaResposta.de(r, hoje());
    }

    @Transactional
    public ReservaResposta cancelar(Long id, Long funcionarioId) {
        Long exemplarId = reservas.exemplarDa(id).orElseThrow(() -> naoEncontrada(id));
        Exemplar exemplar = exemplares.travar(exemplarId).orElseThrow();
        Reserva r = reservas.travar(id).orElseThrow(() -> naoEncontrada(id));
        exigirAguardando(r);
        r.cancelar(CanceladaPor.BIBLIOTECA, funcionarioId, agora());
        exemplar.liberarReserva();
        auditoria.registrar(AcaoAuditoria.RESERVA_CANCELADA, ENTIDADE, r.getId(),
                r.getProtocolo() + " · " + r.getObra().getTitulo() + " · tombo " + exemplar.getTombo());
        return ReservaResposta.de(r, hoje());
    }

    /** Job: aguardando com prazo passado viram {@code EXPIRADA} e o exemplar volta a ficar disponível. */
    @Transactional
    public int expirarVencidas() {
        LocalDate hoje = hoje();
        int expiradas = 0;
        for (Long id : reservas.vencidas(hoje)) {
            Optional<Long> exemplarId = reservas.exemplarDa(id);
            Optional<Exemplar> exemplar = exemplarId.flatMap(exemplares::travar);
            Optional<Reserva> reserva = reservas.travar(id);
            if (exemplar.isPresent() && reserva.isPresent() && reserva.get().aguardando()
                    && reserva.get().getPrazoRetirada().isBefore(hoje)) {
                reserva.get().expirar(agora());
                exemplar.get().liberarReserva();
                expiradas++;
            }
        }
        return expiradas;
    }

    // ---------------------------------------------------------------- apoio

    private Emprestimo retirar(Reserva r, Exemplar exemplar, Long funcionarioId) {
        Emprestimo e = emprestimos.abrirDeReserva(exemplar, r.getLeitor(), funcionarioId);
        r.retirar(e, funcionarioId, agora());
        return e;
    }

    private Leitor identificar(String cpf, LocalDate dataNascimento) {
        String digitos = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || dataNascimento == null) {
            throw new RegraNegocioException(NAO_CONFERE);
        }
        return leitores.findByCpf(digitos)
                .filter(l -> l.getDataNascimento().equals(dataNascimento))
                .orElseThrow(() -> new RegraNegocioException(NAO_CONFERE));
    }

    /**
     * CPF já cadastrado: a data de nascimento precisa conferir (senão, a mesma mensagem de sempre). CPF novo e
     * válido: pré-cadastro com os dados informados, que a equipe confere na retirada. O nome é pedido sempre, para a
     * resposta não revelar se o CPF já é leitor.
     */
    private Leitor identificarOuPreCadastrar(String nome, String cpf, LocalDate dataNascimento, String telefone,
            String email) {
        String digitos = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || dataNascimento == null) {
            throw new RegraNegocioException(NAO_CONFERE);
        }
        Optional<Leitor> existente = leitores.findByCpf(digitos);
        if (existente.isPresent()) {
            return existente.filter(l -> l.getDataNascimento().equals(dataNascimento))
                    .orElseThrow(() -> new RegraNegocioException(NAO_CONFERE));
        }
        if (!Leitor.cpfValido(digitos)) {
            throw new RequisicaoInvalidaException("CPF inválido", "CPF inválido. Confira os números.");
        }
        if (dataNascimento.getYear() < 1900 || !dataNascimento.isBefore(hoje())) {
            throw new RequisicaoInvalidaException("Data de nascimento inválida",
                    "Data de nascimento inválida. Confira o dia, o mês e o ano.");
        }
        String nomeLimpo = nome == null ? "" : nome.strip().replaceAll("\\s+", " ");
        try {
            return leitores.saveAndFlush(Leitor.preCadastro(nomeLimpo, digitos, dataNascimento, telefone, email));
        } catch (DataIntegrityViolationException e) {
            // Outro pedido com o mesmo CPF criou o pré-cadastro ao mesmo tempo.
            throw new ConflitoException("Não foi possível concluir agora. Tente de novo.");
        }
    }

    /** Mesmas regras do balcão, com mensagens para o próprio leitor. */
    private void exigirPodeReservar(Leitor leitor) {
        SituacaoLeitor s = emprestimos.situacao(leitor);
        if (!leitor.isAtivo()) {
            throw new RegraNegocioException("Seu cadastro está inativo. Procure a biblioteca.");
        }
        if (s.bloqueadoAte() != null) {
            throw new RegraNegocioException("Seu cadastro está bloqueado até " + data(s.bloqueadoAte())
                    + " por atraso na devolução.");
        }
        if (s.atrasados() > 0) {
            throw new RegraNegocioException(
                    "Você tem livro com a devolução atrasada. Devolva na biblioteca antes de reservar.");
        }
        if (s.livrosEmUso() >= s.limite()) {
            throw new RegraNegocioException("Você já está com " + s.livrosEmUso() + " de " + s.limite()
                    + " livros permitidos, somando empréstimos e reservas.");
        }
        // Cadastro incompleto (pré-cadastro do site, menor sem responsável) não impede a reserva: a equipe completa
        // na retirada, e o empréstimo só sai com o cadastro em ordem.
    }

    private static void exigirAguardando(Reserva r) {
        switch (r.getStatus()) {
            case AGUARDANDO_RETIRADA -> {
            }
            case RETIRADA -> throw new RegraNegocioException("Esta reserva já foi retirada.");
            case CANCELADA -> throw new RegraNegocioException("Esta reserva foi cancelada.");
            case EXPIRADA -> throw new RegraNegocioException("Esta reserva expirou: o prazo de retirada era "
                    + data(r.getPrazoRetirada()) + ".");
        }
    }

    private String novoProtocolo() {
        String p;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_PROTOCOLO);
            for (int i = 0; i < TAMANHO_PROTOCOLO; i++) {
                sb.append(ALFABETO.charAt(SORTEIO.nextInt(ALFABETO.length())));
            }
            p = sb.toString();
        } while (reservas.existsByProtocolo(p));
        return p;
    }

    /** Aceita minúsculas, espaço e hífen ("abcd-2345"). */
    static String normalizarProtocolo(String protocolo) {
        return protocolo == null ? "" : protocolo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private static RecursoNaoEncontradoException protocoloNaoEncontrado() {
        return new RecursoNaoEncontradoException("Reserva não encontrada. Confira o protocolo.");
    }

    private static RecursoNaoEncontradoException naoEncontrada(Long id) {
        return new RecursoNaoEncontradoException("Reserva " + id + " não encontrada.");
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static String data(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
