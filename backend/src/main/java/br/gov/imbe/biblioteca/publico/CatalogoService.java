package br.gov.imbe.biblioteca.publico;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.acervo.SituacaoExemplar;
import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;

/** Catálogo do site público. */
@Service
public class CatalogoService {

    /** Situações que deixam a obra visível no site. */
    static final Set<SituacaoExemplar> EM_CIRCULACAO =
            EnumSet.of(SituacaoExemplar.DISPONIVEL, SituacaoExemplar.RESERVADO, SituacaoExemplar.EMPRESTADO);

    static final int TAMANHO_MAXIMO = 48;

    private final CatalogoRepository catalogo;

    public CatalogoService(CatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    /** Busca por título, subtítulo, autor ou assunto, em ordem de título. */
    @Transactional(readOnly = true)
    public Pagina<ObraPublica> buscar(String termo, Long assuntoId, boolean soDisponiveis, int pagina, int tamanho) {
        String t = termo == null || termo.isBlank() ? null : termo.strip();
        String padrao = t == null ? null : "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        PageRequest p = PageRequest.of(Math.max(0, pagina), Math.min(TAMANHO_MAXIMO, Math.max(1, tamanho)),
                Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id")));
        return Pagina.de(catalogo.buscar(EM_CIRCULACAO, padrao, assuntoId, soDisponiveis, SituacaoExemplar.DISPONIVEL,
                p), ObraPublica::de);
    }

    @Transactional(readOnly = true)
    public ObraPublica.Detalhe ficha(Long id) {
        return catalogo.visivel(id, EM_CIRCULACAO).map(ObraPublica.Detalhe::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado no catálogo."));
    }

    @Transactional(readOnly = true)
    public List<AssuntoPublico> assuntos() {
        return catalogo.assuntos(EM_CIRCULACAO);
    }
}
