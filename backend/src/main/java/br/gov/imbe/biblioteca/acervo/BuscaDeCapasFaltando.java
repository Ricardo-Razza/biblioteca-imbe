package br.gov.imbe.biblioteca.acervo;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Uma vez por semana, procura capa para as obras que estão sem: capas aparecem nas fontes com o tempo, e obras
 * vindas da importação ou cadastradas à mão também ganham a chance. Sem botão na tela: roda sozinha.
 * <p>
 * Vai devagar (uma obra por vez, com pausa) para não abusar das fontes, e para no limite por execução; o que
 * sobrar fica para a semana seguinte. Foto enviada pela equipe nunca é trocada.
 */
@Component
public class BuscaDeCapasFaltando {

    private static final Logger log = LoggerFactory.getLogger(BuscaDeCapasFaltando.class);

    private final ObraRepository obras;
    private final CapaService capas;
    private final int limite;
    private final Duration pausa;

    public BuscaDeCapasFaltando(ObraRepository obras, CapaService capas,
            @Value("${biblioteca.capas.busca-semanal.limite:500}") int limite,
            @Value("${biblioteca.capas.busca-semanal.pausa:1s}") Duration pausa) {
        this.obras = obras;
        this.capas = capas;
        this.limite = limite;
        this.pausa = pausa;
    }

    /** Domingo de madrugada, quando ninguém está cadastrando. */
    @Scheduled(cron = "${biblioteca.capas.busca-semanal.cron:0 0 4 * * SUN}", zone = "America/Sao_Paulo")
    void agendada() {
        try {
            executar();
        } catch (RuntimeException e) {
            log.error("Busca semanal de capas falhou", e);
        }
    }

    /** @return quantas obras ganharam capa */
    public int executar() {
        var semCapa = obras.semCapa(PageRequest.of(0, limite));
        int achadas = 0;
        for (var obra : semCapa) {
            if (capas.tentarCapaPeloIsbn(obra.getId(), obra.getIsbn13())) {
                achadas++;
            }
            if (!esperar()) {
                break;
            }
        }
        log.info("Busca semanal de capas: {} de {} obra(s) sem capa ganharam capa", achadas, semCapa.size());
        return achadas;
    }

    private boolean esperar() {
        if (pausa.isZero()) {
            return true;
        }
        try {
            Thread.sleep(pausa);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
