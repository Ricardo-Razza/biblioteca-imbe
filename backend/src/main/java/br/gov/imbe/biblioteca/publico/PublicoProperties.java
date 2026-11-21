package br.gov.imbe.biblioteca.publico;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Site público ({@code biblioteca.publico}).
 *
 * @param unidade dados da biblioteca que aparecem no rodapé e na confirmação da reserva
 * @param limites tentativas aceitas antes do 429
 */
@ConfigurationProperties("biblioteca.publico")
public record PublicoProperties(@DefaultValue Unidade unidade, @DefaultValue Limites limites) {

    public record Unidade(
            @DefaultValue("Sala Literária Castelinho") String nome,
            @DefaultValue("") String endereco,
            @DefaultValue("") String horario,
            @DefaultValue("") String telefone,
            @DefaultValue("") String email) {
    }

    /**
     * @param consultasPorMinuto      buscas e fichas por IP a cada minuto
     * @param identificacoesPorIp     reservas, consultas e cancelamentos (CPF + nascimento) por IP na janela
     * @param identificacoesPorCpf    as mesmas tentativas para um mesmo CPF na janela, venham de onde vierem
     * @param janelaIdentificacao     duração da janela das identificações
     */
    public record Limites(
            @DefaultValue("300") int consultasPorMinuto,
            @DefaultValue("20") int identificacoesPorIp,
            @DefaultValue("10") int identificacoesPorCpf,
            @DefaultValue("10m") Duration janelaIdentificacao) {
    }
}
