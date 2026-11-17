package br.gov.imbe.biblioteca.acervo;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;

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

    @Transactional
    public ExemplarResposta cadastrar(Long obraId, ExemplarRequisicao req) {
        Obra obra = obras.findById(obraId).orElseThrow(() -> obraNaoEncontrada(obraId));
        String tombo = normalizarTombo(req.tombo());
        if (exemplares.existsByTombo(tombo)) {
            throw new ConflitoException("Já existe um exemplar com o tombo " + tombo + ".");
        }
        Exemplar exemplar = exemplares.save(new Exemplar(obra, tombo, texto(req.localizacao()), req.estado(),
                req.dataAquisicao(), req.origem()));
        return ExemplarResposta.de(exemplar);
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
