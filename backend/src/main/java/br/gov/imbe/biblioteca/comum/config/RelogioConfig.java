package br.gov.imbe.biblioteca.comum.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Hoje" da biblioteca. Prazos e atrasos são datas do horário de Brasília, mesmo que o servidor
 * (ou o container) esteja em UTC. Os services recebem o {@link Clock} em vez de chamar {@code LocalDate.now()}.
 */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock relogio() {
        return Clock.system(FUSO);
    }
}
