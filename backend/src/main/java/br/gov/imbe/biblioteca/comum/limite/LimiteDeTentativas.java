package br.gov.imbe.biblioteca.comum.limite;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import br.gov.imbe.biblioteca.comum.erro.LimiteExcedidoException;

/**
 * Limite de tentativas por chave (IP, CPF), em janela fixa que começa na primeira tentativa. Fica em memória:
 * vale para uma instância da API, que é como ela roda. As chaves não vão para log.
 */
@Component
public class LimiteDeTentativas {

    /**
     * @param nome    separa os contadores de cada regra
     * @param maximo  tentativas aceitas na janela
     * @param janela  duração da janela
     */
    public record Regra(String nome, int maximo, Duration janela) {
    }

    private static final String MENSAGEM = "Muitas tentativas seguidas. Espere alguns minutos e tente de novo.";

    private final Map<String, Cache<String, AtomicInteger>> contadores = new ConcurrentHashMap<>();

    /** Conta uma tentativa; passando do máximo, lança 429. */
    public void consumir(Regra regra, String chave) {
        Cache<String, AtomicInteger> cache = contadores.computeIfAbsent(regra.nome(), n -> Caffeine.newBuilder()
                .expireAfterWrite(regra.janela())
                .maximumSize(100_000)
                .build());
        if (cache.get(chave, c -> new AtomicInteger()).incrementAndGet() > regra.maximo()) {
            throw new LimiteExcedidoException(MENSAGEM);
        }
    }
}
