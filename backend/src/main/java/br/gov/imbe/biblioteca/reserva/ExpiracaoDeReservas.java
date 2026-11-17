package br.gov.imbe.biblioteca.reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expira as reservas não retiradas no prazo. Roda de hora em hora (e ao subir a API), para não depender de o
 * servidor estar ligado num horário exato: a reserva vence no dia seguinte ao prazo.
 */
@Component
class ExpiracaoDeReservas {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoDeReservas.class);

    private final ReservaService reservas;

    ExpiracaoDeReservas(ReservaService reservas) {
        this.reservas = reservas;
    }

    @EventListener(ApplicationReadyEvent.class)
    void aoSubir() {
        executar();
    }

    @Scheduled(cron = "${biblioteca.reservas.expiracao-cron:0 5 * * * *}", zone = "America/Sao_Paulo")
    void executar() {
        try {
            int n = reservas.expirarVencidas();
            if (n > 0) {
                log.info("{} reserva(s) expirada(s); exemplares liberados", n);
            }
        } catch (RuntimeException e) {
            log.error("Falha ao expirar reservas", e);
        }
    }
}
