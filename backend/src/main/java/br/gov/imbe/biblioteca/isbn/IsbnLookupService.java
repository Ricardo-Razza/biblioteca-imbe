package br.gov.imbe.biblioteca.isbn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Consulta todas as fontes em paralelo (virtual threads) e mescla na ordem de prioridade
 * (BrasilAPI → Google Books → Open Library): a primeira com dados define, as seguintes completam os campos vazios.
 * <p>
 * Cache de 24 h por ISBN, incluindo "não encontrado". Resultado com fonte indisponível não entra no cache,
 * para a próxima busca tentar de novo.
 */
@Service
public class IsbnLookupService {

    private static final Logger log = LoggerFactory.getLogger(IsbnLookupService.class);

    private final List<IsbnProvider> provedores;
    private final Duration esperaMaxima;
    private final Cache<String, ResultadoIsbn> cache;

    /** @param provedores já ordenados por {@code @Order} (prioridade da mescla) */
    public IsbnLookupService(List<IsbnProvider> provedores, IsbnProperties propriedades) {
        this.provedores = List.copyOf(provedores);
        // Cada fonte tem o próprio timeout; a Open Library faz uma segunda rodada (autores), daí a folga.
        this.esperaMaxima = propriedades.timeout().multipliedBy(2).plusSeconds(1);
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(propriedades.cacheTtl())
                .maximumSize(10_000)
                .build();
    }

    public ResultadoIsbn buscar(Isbn isbn) {
        ResultadoIsbn emCache = cache.getIfPresent(isbn.valor());
        if (emCache != null) {
            return emCache;
        }
        ResultadoIsbn resultado = consultarFontes(isbn);
        if (resultado.completo()) {
            cache.put(isbn.valor(), resultado);
        }
        return resultado;
    }

    private ResultadoIsbn consultarFontes(Isbn isbn) {
        long inicio = System.nanoTime();
        Map<FonteIsbn, Optional<DadosLivro>> respostas = new LinkedHashMap<>();
        List<FonteIsbn> indisponiveis = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<IsbnProvider, Future<Optional<DadosLivro>>> futuros = new LinkedHashMap<>();
            provedores.forEach(p -> futuros.put(p, executor.submit(() -> p.buscar(isbn))));

            long prazo = System.nanoTime() + esperaMaxima.toNanos();
            futuros.forEach((provedor, futuro) -> {
                try {
                    long restante = Math.max(0, prazo - System.nanoTime());
                    respostas.put(provedor.fonte(), futuro.get(restante, TimeUnit.NANOSECONDS));
                } catch (TimeoutException e) {
                    futuro.cancel(true);
                    indisponivel(isbn, provedor.fonte(), "sem resposta no prazo", indisponiveis);
                } catch (ExecutionException e) {
                    indisponivel(isbn, provedor.fonte(), e.getCause().getMessage(), indisponiveis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    indisponivel(isbn, provedor.fonte(), "interrompida", indisponiveis);
                }
            });
        }

        Optional<DadosLivro> mesclado = Optional.empty();
        List<FonteIsbn> comDados = new ArrayList<>();
        for (Map.Entry<FonteIsbn, Optional<DadosLivro>> r : respostas.entrySet()) {
            if (r.getValue().isPresent()) {
                comDados.add(r.getKey());
                DadosLivro dados = r.getValue().get();
                mesclado = Optional.of(mesclado.map(atual -> atual.completadoCom(dados)).orElse(dados));
            }
        }
        mesclado = mesclado.map(d -> d.comCapas(capasEmOrdem(respostas)));

        log.info("ISBN {}: {} em {} ms (com dados: {}; indisponíveis: {})", isbn,
                mesclado.isPresent() ? "encontrado" : "não encontrado",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio), comDados, indisponiveis);
        return new ResultadoIsbn(isbn, mesclado, comDados, indisponiveis);
    }

    /**
     * Capas de todas as fontes, pela qualidade costumeira e não pela prioridade dos dados: Open Library (imagem
     * grande), BrasilAPI e por último Google Books (miniatura menor). Quem baixa tenta em ordem até uma servir.
     */
    static List<String> capasEmOrdem(Map<FonteIsbn, Optional<DadosLivro>> respostas) {
        return ORDEM_CAPAS.stream()
                .flatMap(f -> respostas.getOrDefault(f, Optional.empty()).stream())
                .flatMap(d -> d.capas().stream())
                .distinct()
                .toList();
    }

    private static final List<FonteIsbn> ORDEM_CAPAS =
            List.of(FonteIsbn.OPEN_LIBRARY, FonteIsbn.BRASILAPI, FonteIsbn.GOOGLE_BOOKS);

    private static void indisponivel(Isbn isbn, FonteIsbn fonte, String motivo, List<FonteIsbn> indisponiveis) {
        log.warn("ISBN {}: fonte {} indisponível: {}", isbn, fonte, motivo);
        indisponiveis.add(fonte);
    }
}
