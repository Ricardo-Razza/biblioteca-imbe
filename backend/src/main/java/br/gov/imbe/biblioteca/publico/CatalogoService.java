package br.gov.imbe.biblioteca.publico;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.acervo.Obra;
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

    /** "Mais retirados" do site: autores mais emprestados nesta janela. */
    static final int DIAS_MAIS_RETIRADOS = 90;
    static final int QUANTOS_MAIS_RETIRADOS = 3;

    private final CatalogoRepository catalogo;
    private final RegistroDeBuscas registro;
    private final Clock relogio;

    public CatalogoService(CatalogoRepository catalogo, RegistroDeBuscas registro, Clock relogio) {
        this.catalogo = catalogo;
        this.registro = registro;
        this.relogio = relogio;
    }

    /** Ordem do catálogo: por título (padrão) ou pelos cadastrados mais recentemente. */
    public enum Ordem {
        TITULO(Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id"))),
        RECENTES(Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")));

        final Sort sort;

        Ordem(Sort sort) {
            this.sort = sort;
        }

        /** {@code "recentes"} ou qualquer outra coisa (título). */
        public static Ordem de(String valor) {
            return "recentes".equalsIgnoreCase(valor) ? RECENTES : TITULO;
        }
    }

    /** Busca por título, subtítulo, autor ou assunto. */
    @Transactional(readOnly = true)
    public Pagina<ObraPublica> buscar(String termo, Long assuntoId, boolean soDisponiveis, Ordem ordem, int pagina,
            int tamanho) {
        String t = termo == null || termo.isBlank() ? null : termo.strip();
        String padrao = t == null ? null : "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        PageRequest p = PageRequest.of(Math.max(0, pagina), Math.min(TAMANHO_MAXIMO, Math.max(1, tamanho)),
                ordem.sort);
        Page<Obra> resultado = catalogo.buscar(EM_CIRCULACAO, padrao, assuntoId, soDisponiveis,
                SituacaoExemplar.DISPONIVEL, p);
        // Só a busca pura que não achou nada vira sinal para a compra (com filtro, o zero pode ser do filtro).
        if (t != null && assuntoId == null && !soDisponiveis && p.getPageNumber() == 0 && resultado.isEmpty()) {
            registro.semResultado(t, LocalDate.now(relogio));
        }
        return Pagina.de(resultado, ObraPublica::de);
    }

    /** Autores mais emprestados nos últimos {@value #DIAS_MAIS_RETIRADOS} dias (vazio sem empréstimos). */
    @Transactional(readOnly = true)
    public List<String> maisRetirados() {
        LocalDate desde = LocalDate.now(relogio).minusDays(DIAS_MAIS_RETIRADOS - 1);
        return catalogo.autoresMaisRetirados(desde, EM_CIRCULACAO, PageRequest.of(0, QUANTOS_MAIS_RETIRADOS + 2))
                .stream()
                // Fontes de ISBN às vezes trazem "[author not identified]" e afins.
                .filter(nome -> !nome.isBlank() && !nome.startsWith("["))
                .limit(QUANTOS_MAIS_RETIRADOS)
                .toList();
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
