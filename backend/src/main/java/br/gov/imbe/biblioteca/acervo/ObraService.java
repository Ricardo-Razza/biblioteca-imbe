package br.gov.imbe.biblioteca.acervo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.Isbn;

@Service
public class ObraService {

    private final ObraRepository obras;
    private final AutorRepository autores;
    private final AssuntoRepository assuntos;

    public ObraService(ObraRepository obras, AutorRepository autores, AssuntoRepository assuntos) {
        this.obras = obras;
        this.autores = autores;
        this.assuntos = assuntos;
    }

    @Transactional
    public ObraResposta cadastrar(ObraRequisicao req) {
        Isbn isbn = Isbn.de(req.isbn());
        if (obras.existsByIsbn13(isbn.valor())) {
            throw isbnJaCadastrado(isbn);
        }
        Obra obra = obras.save(new Obra(isbn.valor(), texto(req.titulo())));
        aplicar(obra, isbn, req);
        return ObraResposta.de(obra);
    }

    @Transactional
    public ObraResposta alterar(Long id, ObraRequisicao req) {
        Obra obra = buscarEntidade(id);
        Isbn isbn = Isbn.de(req.isbn());
        if (obras.existsByIsbn13AndIdNot(isbn.valor(), id)) {
            throw isbnJaCadastrado(isbn);
        }
        aplicar(obra, isbn, req);
        return ObraResposta.de(obra);
    }

    @Transactional(readOnly = true)
    public ObraResposta buscar(Long id) {
        return ObraResposta.de(buscarEntidade(id));
    }

    /** Para o cadastro por ISBN: se a obra já existe, a tela só oferece "Adicionar exemplar". */
    @Transactional(readOnly = true)
    public Optional<Long> idPorIsbn(Isbn isbn) {
        return obras.findByIsbn13(isbn.valor()).map(Obra::getId);
    }

    /**
     * Lista o acervo. Termo com cara de ISBN (10 ou 13 dígitos válidos) busca pelo ISBN; senão, por trecho
     * do título ou do autor.
     */
    @Transactional(readOnly = true)
    public Pagina<ObraResposta> listar(String termo, boolean semCapa, Pageable paginacao) {
        String t = texto(termo);
        if (t == null) {
            return Pagina.de(semCapa ? obras.findByCapaArquivoIsNull(paginacao) : obras.findAll(paginacao),
                    ObraResposta::de);
        }
        Optional<Isbn> isbn = comoIsbn(t);
        if (isbn.isPresent()) {
            List<Obra> achadas = obras.findByIsbn13(isbn.get().valor())
                    .filter(o -> !semCapa || o.getCapaArquivo() == null)
                    .map(List::of).orElse(List.of());
            return new Pagina<>(achadas.stream().map(ObraResposta::de).toList(), 0, paginacao.getPageSize(),
                    achadas.size(), achadas.isEmpty() ? 0 : 1);
        }
        String padrao = "%" + t.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return Pagina.de(obras.buscarPorTituloOuAutor(padrao, semCapa, paginacao), ObraResposta::de);
    }

    private void aplicar(Obra obra, Isbn isbn, ObraRequisicao req) {
        obra.preencher(isbn.valor(), texto(req.titulo()), texto(req.subtitulo()), texto(req.editora()),
                req.ano() == null ? null : req.ano().shortValue(), req.paginas(),
                texto(req.idioma()) == null ? null : texto(req.idioma()).toLowerCase(Locale.ROOT),
                texto(req.sinopse()), req.fonteDados() == null ? FonteIsbn.MANUAL : req.fonteDados());
        obra.definirAutores(semRepetir(req.autores()).stream()
                .map(nome -> autores.findByNome(nome).orElseGet(() -> autores.save(new Autor(nome))))
                .toList());
        Set<Assunto> novos = new LinkedHashSet<>();
        for (String nome : semRepetir(req.assuntos())) {
            novos.add(assuntos.findByNome(nome).orElseGet(() -> assuntos.save(new Assunto(nome))));
        }
        obra.definirAssuntos(novos);
    }

    private Obra buscarEntidade(Long id) {
        return obras.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Obra " + id + " não encontrada."));
    }

    private static ConflitoException isbnJaCadastrado(Isbn isbn) {
        return new ConflitoException("Já existe uma obra com o ISBN " + isbn + ". Adicione um exemplar a ela.");
    }

    private static Optional<Isbn> comoIsbn(String termo) {
        String digitos = termo.replaceAll("[\\s-]", "");
        if (!digitos.matches("\\d{13}|\\d{9}[\\dXx]")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Isbn.de(digitos));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Remove brancos e repetidos sem diferenciar maiúsculas nem acentos, como o banco faria:
     * mandar "José" e "JOSE" juntos daria duas buscas pelo mesmo autor.
     */
    private static List<String> semRepetir(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.PRIMARY);
        List<String> resultado = new ArrayList<>();
        for (String v : valores) {
            String t = texto(v);
            if (t != null && resultado.stream().noneMatch(r -> collator.compare(r, t) == 0)) {
                resultado.add(t);
            }
        }
        return resultado;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
