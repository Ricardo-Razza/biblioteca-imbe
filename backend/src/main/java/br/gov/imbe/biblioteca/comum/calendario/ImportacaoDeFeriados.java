package br.gov.imbe.biblioteca.comum.calendario;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Garante os feriados nacionais do ano atual e do próximo: ao subir a API (numa thread à parte, para não atrasar a
 * subida) e todo dia de madrugada. Só chama a BrasilAPI para o ano que ainda não tem nenhum nacional; fora do ar,
 * tenta de novo no dia seguinte (e o administrador pode importar na tela de feriados).
 */
@Component
class ImportacaoDeFeriados {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoDeFeriados.class);

    private final FeriadoService feriados;
    private final Clock relogio;

    ImportacaoDeFeriados(FeriadoService feriados, Clock relogio) {
        this.feriados = feriados;
        this.relogio = relogio;
    }

    @EventListener(ApplicationReadyEvent.class)
    void aoSubir() {
        Thread.ofVirtual().name("importacao-feriados").start(this::garantir);
    }

    @Scheduled(cron = "${biblioteca.feriados.cron:0 10 3 * * *}", zone = "America/Sao_Paulo")
    void garantir() {
        int ano = LocalDate.now(relogio).getYear();
        for (int a = ano; a <= ano + 1; a++) {
            try {
                if (!feriados.temNacionais(a)) {
                    FeriadoService.Importacao r = feriados.importarNacionais(a);
                    log.info("Feriados nacionais de {} importados: {} novos", a, r.novos());
                }
            } catch (RuntimeException e) {
                log.warn("Feriados nacionais de {} não importados: {}", a, e.getMessage());
            }
        }
    }
}
