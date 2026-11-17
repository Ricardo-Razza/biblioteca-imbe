package br.gov.imbe.biblioteca.isbn.fonte;

import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.FonteIsbnIndisponivelException;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.IsbnConfig;
import br.gov.imbe.biblioteca.isbn.IsbnProperties;
import br.gov.imbe.biblioteca.isbn.IsbnProvider;

/**
 * Google Books: {@code GET /books/v1/volumes?q=isbn:{isbn}&key={chave}}.
 * A chave é opcional (sem ela a cota é menor) e nunca vai para log nem mensagem de erro.
 */
@Component
@Order(2)
public class GoogleBooksProvider implements IsbnProvider {

    private final RestClient cliente;
    private final IsbnProperties.GoogleBooks config;

    public GoogleBooksProvider(RestClient.Builder builder, IsbnProperties propriedades) {
        this.config = propriedades.googleBooks();
        this.cliente = IsbnConfig.clientePara(builder, config.url().toString(), propriedades.timeout());
    }

    @Override
    public FonteIsbn fonte() {
        return FonteIsbn.GOOGLE_BOOKS;
    }

    @Override
    public Optional<DadosLivro> buscar(Isbn isbn) {
        try {
            return cliente.get()
                    .uri(uri -> montarUri(uri, isbn))
                    .exchange((requisicao, resposta) -> {
                        if (resposta.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            return Optional.<DadosLivro>empty();
                        }
                        if (!resposta.getStatusCode().is2xxSuccessful()) {
                            throw new FonteIsbnIndisponivelException(fonte(),
                                    "HTTP " + resposta.getStatusCode().value(), null);
                        }
                        Resposta corpo = resposta.bodyTo(Resposta.class);
                        return Optional.ofNullable(corpo)
                                .flatMap(c -> c.items() == null ? Optional.empty() : c.items().stream().findFirst())
                                .map(Item::volumeInfo)
                                .map(v -> converter(isbn, v))
                                .filter(d -> d.titulo() != null);
                    });
        } catch (RestClientException e) {
            throw FonteIsbnIndisponivelException.de(fonte(), e);
        }
    }

    private java.net.URI montarUri(UriBuilder uri, Isbn isbn) {
        uri.path("/books/v1/volumes").queryParam("q", "isbn:" + isbn.valor());
        if (config.temChave()) {
            uri.queryParam("key", config.chave());
        }
        return uri.build();
    }

    private DadosLivro converter(Isbn isbn, VolumeInfo v) {
        String capa = v.imageLinks() == null ? null
                : Textos.https(v.imageLinks().thumbnail() != null ? v.imageLinks().thumbnail()
                        : v.imageLinks().smallThumbnail());
        return new DadosLivro(isbn.valor(), v.title(), v.subtitle(), v.authors(), v.publisher(),
                Textos.ano(v.publishedDate()), v.pageCount(), Textos.idioma(v.language()), v.description(),
                v.categories(), capa, fonte());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(Integer totalItems, List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(VolumeInfo volumeInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VolumeInfo(String title, String subtitle, List<String> authors, String publisher, String publishedDate,
            String description, Integer pageCount, List<String> categories, String language, ImageLinks imageLinks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageLinks(String smallThumbnail, String thumbnail) {
    }
}
