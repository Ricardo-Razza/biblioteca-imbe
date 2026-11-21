package br.gov.imbe.biblioteca.acervo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

@Service
public class ExemplarService {

    private final ExemplarRepository exemplares;
    private final ObraRepository obras;
    private final Auditoria auditoria;

    public ExemplarService(ExemplarRepository exemplares, ObraRepository obras, Auditoria auditoria) {
        this.exemplares = exemplares;
        this.obras = obras;
        this.auditoria = auditoria;
    }

    /**
     * Cadastra o exemplar. Sem tombo, o sistema numera: grava com um tombo provisório único e, já com o id,
     * troca por {@code AUTO-000123}, tudo na mesma transação.
     */
    @Transactional
    public ExemplarResposta cadastrar(Long obraId, ExemplarRequisicao req) {
        Obra obra = obras.findById(obraId).orElseThrow(() -> obraNaoEncontrada(obraId));
        String tombo = normalizarTombo(req.tombo());
        boolean automatico = tombo == null || tombo.isEmpty();
        if (automatico) {
            tombo = PREFIXO_PROVISORIO + UUID.randomUUID().toString().replace("-", "").substring(0, 26);
        } else if (tombo.startsWith(PREFIXO_AUTOMATICO) || tombo.startsWith(PREFIXO_PROVISORIO)) {
            throw new RequisicaoInvalidaException("Tombo reservado",
                    "Tombos que começam com " + PREFIXO_AUTOMATICO + " são gerados pelo sistema. "
                            + "Deixe o campo em branco para o sistema numerar.");
        } else if (exemplares.existsByTombo(tombo)) {
            throw new ConflitoException("Já existe um exemplar com o tombo " + tombo + ".");
        }
        Exemplar exemplar = exemplares.saveAndFlush(new Exemplar(obra, tombo, texto(req.localizacao()),
                req.estado(), req.dataAquisicao(), req.origem()));
        if (automatico) {
            exemplar.numerarAutomaticamente();
            exemplares.flush();
        }
        return ExemplarResposta.de(exemplar);
    }

    /** Prefixo dos tombos gerados pelo sistema. */
    public static final String PREFIXO_AUTOMATICO = "AUTO-";
    static final String PREFIXO_PROVISORIO = "TMP-";

    static String tomboAutomatico(Long id) {
        return PREFIXO_AUTOMATICO + String.format(Locale.ROOT, "%06d", id);
    }

    /** Todos os exemplares da obra, inclusive os baixados, em ordem de tombo. */
    @Transactional(readOnly = true)
    public List<ExemplarResposta> daObra(Long obraId) {
        if (!obras.existsById(obraId)) {
            throw obraNaoEncontrada(obraId);
        }
        return exemplares.findByObraIdOrderByTombo(obraId).stream().map(ExemplarResposta::de).toList();
    }

    @Transactional(readOnly = true)
    public ExemplarResposta porTombo(String tombo) {
        String t = normalizarTombo(tombo);
        return exemplares.findByTombo(t).map(ExemplarResposta::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum exemplar com o tombo " + t + "."));
    }

    /** Máximo de exemplares na busca do balcão. */
    static final int LIMITE_BUSCA_BALCAO = 20;

    /**
     * Busca do balcão para emprestar: nem todo livro tem etiqueta, então o termo pode ser o tombo, o ISBN, um
     * trecho do título ou o nome de um autor. Só traz o que pode sair (disponível ou reservado; a reserva de
     * outro leitor o próprio empréstimo recusa). Termo com menos de 2 letras devolve vazio.
     */
    @Transactional(readOnly = true)
    public List<ExemplarResposta> buscarParaEmprestimo(String termo) {
        String t = termo == null ? "" : termo.strip();
        if (t.length() < 2) {
            return List.of();
        }
        String isbn = ObraService.comoIsbn(t).map(i -> i.valor()).orElse(null);
        String padrao = "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return exemplares.buscarNoBalcao(normalizarTombo(t), isbn, padrao,
                List.of(SituacaoExemplar.DISPONIVEL, SituacaoExemplar.RESERVADO),
                PageRequest.of(0, LIMITE_BUSCA_BALCAO)).stream().map(ExemplarResposta::de).toList();
    }

    /** Localização e estado primeiro, depois a situação: um exemplar emprestado ainda pode mudar de estante. */
    @Transactional
    public ExemplarResposta alterar(Long id, ExemplarAlteracao alt) {
        Exemplar exemplar = exemplares.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar " + id + " não encontrado."));
        if (alt.localizacao() != null || alt.estado() != null) {
            String localizacao = alt.localizacao() == null ? exemplar.getLocalizacao() : texto(alt.localizacao());
            exemplar.alterar(localizacao, alt.estado());
        }
        if (alt.situacao() != null && alt.situacao() != exemplar.getSituacao()) {
            SituacaoExemplar antes = exemplar.getSituacao();
            exemplar.mudarSituacao(alt.situacao());
            auditoria.registrar(AcaoAuditoria.EXEMPLAR_ALTERADO, "EXEMPLAR", id,
                    "Tombo " + exemplar.getTombo() + ": " + antes + " → " + alt.situacao());
        }
        return ExemplarResposta.de(exemplar);
    }

    public static String normalizarTombo(String tombo) {
        return tombo == null ? null : tombo.strip().toUpperCase(Locale.ROOT);
    }

    private static RecursoNaoEncontradoException obraNaoEncontrada(Long id) {
        return new RecursoNaoEncontradoException("Obra " + id + " não encontrada.");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
