package br.gov.imbe.biblioteca.importacao;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import br.gov.imbe.biblioteca.acervo.CapaService;
import br.gov.imbe.biblioteca.acervo.ExemplarRepository;
import br.gov.imbe.biblioteca.acervo.ExemplarRequisicao;
import br.gov.imbe.biblioteca.acervo.ExemplarService;
import br.gov.imbe.biblioteca.acervo.ObraResposta;
import br.gov.imbe.biblioteca.acervo.ObraService;
import br.gov.imbe.biblioteca.comum.erro.BibliotecaException;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;


import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.IsbnLookupService;
import br.gov.imbe.biblioteca.isbn.ResultadoIsbn;
import jakarta.annotation.PreDestroy;

/**
 * Processa as planilhas uma de cada vez, linha por linha, numa thread própria: busca o ISBN nas fontes (fora de
 * transação, é rede) e cadastra obra e exemplar numa transação curta por linha. Entre uma consulta externa e
 * outra espera {@code biblioteca.importacao.intervalo}, para não esgotar a cota das APIs.
 * <p>
 * Se a aplicação parar no meio, as linhas que faltam continuam pendentes e o processamento recomeça ao subir.
 */
@Component
public class ProcessadorDeImportacao {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorDeImportacao.class);

    private final ImportacaoRepository importacoes;
    private final ItemImportacaoRepository itens;
    private final ObraService obras;
    private final ExemplarService exemplares;
    private final ExemplarRepository exemplarRepository;
    private final CapaService capas;
    private final IsbnLookupService isbn;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final Duration intervalo;

    private final ExecutorService fila = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("importacao").factory());
    private final Set<Long> agendadas = ConcurrentHashMap.newKeySet();

    ProcessadorDeImportacao(ImportacaoRepository importacoes, ItemImportacaoRepository itens, ObraService obras, ExemplarService exemplares,
            ExemplarRepository exemplarRepository, CapaService capas, IsbnLookupService isbn,
            TransactionTemplate transacao, Clock relogio,
            @Value("${biblioteca.importacao.intervalo:500ms}") Duration intervalo) {
        this.importacoes = importacoes;
        this.itens = itens;
        this.obras = obras;
        this.exemplares = exemplares;
        this.exemplarRepository = exemplarRepository;
        this.capas = capas;
        this.isbn = isbn;
        this.transacao = transacao;
        this.relogio = relogio;
        this.intervalo = intervalo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void retomar() {
        for (Importacao i : importacoes.findByStatusOrderById(Importacao.Status.PROCESSANDO)) {
            log.info("Retomando a importação {} ({})", i.getId(), i.getArquivo());
            agendar(i.getId());
        }
    }

    @PreDestroy
    void parar() {
        fila.shutdownNow();
    }

    /** Põe na fila. Chamar só depois do commit que gravou as linhas. */
    void agendar(Long importacaoId) {
        if (!agendadas.add(importacaoId)) {
            return;
        }
        fila.submit(() -> {
            try {
                processarTudo(importacaoId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.error("Importação {} parou: {}", importacaoId, e.toString(), e);
            } finally {
                agendadas.remove(importacaoId);
            }
        });
    }

    /** Processa até acabar a fila da importação e a marca como concluída. Também usado direto pelos testes. */
    void processarTudo(Long importacaoId) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            Optional<ItemImportacao> proximo = itens.findFirstByImportacaoIdAndResultadoOrderByLinha(importacaoId,
                    ResultadoItem.PENDENTE);
            if (proximo.isEmpty()) {
                transacao.executeWithoutResult(s -> importacoes.findById(importacaoId)
                        .ifPresent(i -> i.concluir(LocalDateTime.now(relogio))));
                return;
            }
            if (processar(proximo.get()) && !intervalo.isZero()) {
                Thread.sleep(intervalo);
            }
        }
    }

    /** @return se consultou as fontes externas */
    private boolean processar(ItemImportacao item) {
        Isbn alvo = Isbn.de(item.getIsbn13());
        try {
            Optional<Long> existente = obras.idPorIsbn(alvo);
            if (existente.isPresent()) {
                if (item.getTombo() == null) {
                    registrar(item.getId(), ResultadoItem.JA_CADASTRADO, existente.get(), "A obra já estava no acervo.");
                } else {
                    transacao.executeWithoutResult(s -> {
                        incluirExemplar(existente.get(), item);
                        atualizar(item.getId(), ResultadoItem.EXEMPLAR_ADICIONADO, existente.get(), null);
                    });
                }
                return false;
            }

            ResultadoIsbn resultado = isbn.buscar(alvo);
            if (resultado.dados().isEmpty()) {
                if (resultado.completo()) {
                    registrar(item.getId(), ResultadoItem.NAO_ENCONTRADO, null,
                            "Nenhuma fonte conhece este ISBN. Cadastre pela tela de novo livro.");
                } else {
                    registrar(item.getId(), ResultadoItem.FONTE_INDISPONIVEL, null,
                            "Fora do ar: " + nomes(resultado.fontesIndisponiveis()) + ". Tente de novo mais tarde.");
                }
                return true;
            }

            ObraResposta obra = transacao.execute(s -> {
                if (item.getTombo() != null && exemplarRepository.existsByTombo(item.getTombo())) {
                    throw tomboExistente(item.getTombo());
                }
                ObraResposta nova = obras.cadastrar(ObraDaFonte.de(resultado.dados().get()));
                if (item.getTombo() != null) {
                    incluirExemplar(nova.id(), item);
                }
                atualizar(item.getId(), ResultadoItem.CADASTRADO, nova.id(), null);
                return nova;
            });
            capas.tentarCapaDaFonte(obra);
            return true;
        } catch (BibliotecaException e) {
            registrar(item.getId(), ResultadoItem.ERRO, null, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Importação {}, linha {}: {}", item.getImportacaoId(), item.getLinha(), e.toString(), e);
            registrar(item.getId(), ResultadoItem.ERRO, null, "Erro inesperado ao cadastrar esta linha.");
        }
        return false;
    }

    private void incluirExemplar(Long obraId, ItemImportacao item) {
        // Confere antes de gravar qualquer coisa: a transação inteira desiste sem deixar obra órfã
        if (exemplarRepository.existsByTombo(item.getTombo())) {
            throw tomboExistente(item.getTombo());
        }
        exemplares.cadastrar(obraId, new ExemplarRequisicao(item.getTombo(), item.getLocalizacao(), null, null, null));
    }

    static String nomes(List<FonteIsbn> fontes) {
        return fontes.stream().map(f -> switch (f) {
            case BRASILAPI -> "BrasilAPI";
            case GOOGLE_BOOKS -> "Google Books";
            case OPEN_LIBRARY -> "Open Library";
            case MANUAL -> "manual";
        }).collect(Collectors.joining(", "));
    }

    private static ConflitoException tomboExistente(String tombo) {
        return new ConflitoException("Já existe um exemplar com o tombo " + tombo + ".");
    }

    private void registrar(Long itemId, ResultadoItem resultado, Long obraId, String mensagem) {
        transacao.executeWithoutResult(s -> atualizar(itemId, resultado, obraId, mensagem));
    }

    private void atualizar(Long itemId, ResultadoItem resultado, Long obraId, String mensagem) {
        itens.findById(itemId).orElseThrow().registrar(resultado, obraId, mensagem);
    }
}
