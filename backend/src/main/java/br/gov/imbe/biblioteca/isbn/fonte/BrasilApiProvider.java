package br.gov.imbe.biblioteca.isbn.fonte;

import java.util.List;
import java.util.Optional;

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
 * BrasilAPI: {@code GET /api/isbn/v1/{isbn}}. Sem chave. Agrega CBL, Mercado Editorial, Open Library e Google Books.
 * Primeira fonte consultada: é a que melhor conhece livros brasileiros.
 */
@Component
@Order(1)
public class BrasilApiProvider implements IsbnProvider {

    private final RestClient cliente;

    public BrasilApiProvider(RestClient.Builder builder, IsbnProperties propriedades) {
        this.cliente = IsbnConfig.clientePara(builder, propriedades.brasilapi().url().toString(),
                propriedades.timeout());
    }

    @Override
    public FonteIsbn fonte() {
        return FonteIsbn.BRASILAPI;
    }

    @Override
    public Optional<DadosLivro> buscar(Isbn isbn) {
        try {
            return cliente.get()
                    .uri("/api/isbn/v1/{isbn}", isbn.valor())
                    .exchange((requisicao, resposta) -> {
                        if (resposta.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            return Optional.<DadosLivro>empty();
                        }
                        if (!resposta.getStatusCode().is2xxSuccessful()) {
                            throw new FonteIsbnIndisponivelException(fonte(),
                                    "HTTP " + resposta.getStatusCode().value(), null);
                        }
                        Resposta corpo = resposta.bodyTo(Resposta.class);
                        return Optional.ofNullable(corpo).map(c -> converter(isbn, c)).filter(d -> d.titulo() != null);
                    });
        } catch (RestClientException e) {
            throw FonteIsbnIndisponivelException.de(fonte(), e);
        }
    }

    private DadosLivro converter(Isbn isbn, Resposta r) {
        return new DadosLivro(isbn.valor(), r.title(), r.subtitle(), r.authors(), r.publisher(), r.year(),
                r.pageCount(), null, r.synopsis(), r.subjects(),
                r.coverUrl() == null ? List.of() : List.of(r.coverUrl()), fonte());
    }

    /** Só os campos usados. A BrasilAPI não informa o idioma. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(
            String title,
            String subtitle,
            List<String> authors,
            String publisher,
            String synopsis,
            Integer year,
            @JsonProperty("page_count") Integer pageCount,
            List<String> subjects,
            @JsonProperty("cover_url") String coverUrl) {
    }
}
