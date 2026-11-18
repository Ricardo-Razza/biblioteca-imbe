package br.gov.imbe.biblioteca.isbn.fonte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.FonteIsbnIndisponivelException;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.IsbnConfig;
import br.gov.imbe.biblioteca.isbn.IsbnProperties;
import br.gov.imbe.biblioteca.isbn.IsbnProvider;

/**
 * Open Library: {@code GET /isbn/{isbn}.json} (redireciona para {@code /books/OL...M.json}).
 * A edição traz só a chave dos autores, então os nomes vêm de {@code /authors/{id}.json}, em paralelo.
 * Capa em {@code covers.openlibrary.org/b/isbn/{isbn}-L.jpg?default=false}, oferecida sempre que a edição existe.
 */
@Component
@Order(3)
public class OpenLibraryProvider implements IsbnProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryProvider.class);
    private static final int MAX_AUTORES = 10;

    private final RestClient cliente;
    private final String capasUrl;

    public OpenLibraryProvider(RestClient.Builder builder, IsbnProperties propriedades) {
        this.cliente = IsbnConfig.clientePara(builder, propriedades.openLibrary().url().toString(),
                propriedades.timeout());
        this.capasUrl = propriedades.openLibrary().capasUrl().toString().replaceAll("/+$", "");
    }

    @Override
    public FonteIsbn fonte() {
        return FonteIsbn.OPEN_LIBRARY;
    }

    @Override
    public Optional<DadosLivro> buscar(Isbn isbn) {
        Optional<Edicao> edicao;
        try {
            edicao = cliente.get()
                    .uri("/isbn/{isbn}.json", isbn.valor())
                    .exchange((requisicao, resposta) -> {
                        if (resposta.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            return Optional.<Edicao>empty();
                        }
                        if (!resposta.getStatusCode().is2xxSuccessful()) {
                            throw new FonteIsbnIndisponivelException(fonte(),
                                    "HTTP " + resposta.getStatusCode().value(), null);
                        }
                        return Optional.ofNullable(resposta.bodyTo(Edicao.class));
                    });
        } catch (RestClientException e) {
            throw FonteIsbnIndisponivelException.de(fonte(), e);
        }
        return edicao.map(e -> converter(isbn, e)).filter(d -> d.titulo() != null);
    }

    private DadosLivro converter(Isbn isbn, Edicao e) {
        // Sempre oferece a capa pelo ISBN: o serviço de capas costuma ter a imagem mesmo quando a edição não lista
        // nenhuma. Sem capa, ele responde 404 (default=false) e quem baixa passa para a próxima candidata.
        List<String> capa = List.of(capasUrl + "/b/isbn/" + isbn.valor() + "-L.jpg?default=false");
        String idioma = e.languages() == null ? null
                : e.languages().stream().map(Chave::key).filter(k -> k != null)
                        .map(k -> k.substring(k.lastIndexOf('/') + 1)).findFirst().map(Textos::idioma).orElse(null);
        String editora = e.publishers() == null ? null : e.publishers().stream().findFirst().orElse(null);
        return new DadosLivro(isbn.valor(), e.title(), e.subtitle(), nomesDosAutores(e.authors()), editora,
                Textos.ano(e.publishDate()),
                e.numberOfPages() != null ? e.numberOfPages() : Textos.paginas(e.pagination()),
                idioma, descricao(e.description()), e.subjects(),
                capa, fonte());
    }

    /** Busca os nomes em paralelo. Autor que falhar fica de fora; o resto da edição continua valendo. */
    private List<String> nomesDosAutores(List<Chave> autores) {
        if (autores == null || autores.isEmpty()) {
            return List.of();
        }
        List<String> chaves = autores.stream().map(Chave::key)
                .filter(k -> k != null && k.startsWith("/authors/")).limit(MAX_AUTORES).toList();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futuros = chaves.stream().map(k -> executor.submit(() -> nomeDoAutor(k))).toList();
            List<String> nomes = new ArrayList<>();
            for (Future<String> f : futuros) {
                try {
                    String nome = f.get();
                    if (nome != null) {
                        nomes.add(nome);
                    }
                } catch (Exception ex) {
                    log.debug("Open Library: autor ignorado ({})", ex.toString());
                }
            }
            return nomes;
        }
    }

    private String nomeDoAutor(String chave) {
        try {
            Autor autor = cliente.get().uri(chave + ".json").retrieve().body(Autor.class);
            return autor == null ? null : autor.name();
        } catch (RestClientException e) {
            log.debug("Open Library: falha ao buscar {}: {}", chave, e.getMostSpecificCause().toString());
            return null;
        }
    }

    /** A descrição vem como texto ou como {"type": "/type/text", "value": "..."}. */
    private static String descricao(Object valor) {
        if (valor instanceof String texto) {
            return texto;
        }
        if (valor instanceof Map<?, ?> mapa && mapa.get("value") instanceof String texto) {
            return texto;
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Edicao(
            String title,
            String subtitle,
            List<String> publishers,
            @JsonProperty("publish_date") String publishDate,
            @JsonProperty("number_of_pages") Integer numberOfPages,
            /** Texto livre de catálogo, ex.: "417 p. :", "xii, 300 p." */
            String pagination,
            List<Chave> authors,
            List<Chave> languages,
            List<String> subjects,
            Object description,
            List<Long> covers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Chave(String key) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Autor(String name) {
    }
}
