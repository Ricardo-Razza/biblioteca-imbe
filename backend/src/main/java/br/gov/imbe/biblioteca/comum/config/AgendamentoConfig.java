package br.gov.imbe.biblioteca.comum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tarefas agendadas: expiração de reservas e feriados nacionais do ano. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AgendamentoConfig {
}
