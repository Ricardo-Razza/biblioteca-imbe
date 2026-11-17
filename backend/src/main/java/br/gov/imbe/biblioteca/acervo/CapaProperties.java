package br.gov.imbe.biblioteca.acervo;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Capas ({@code biblioteca.capas.*}).
 *
 * @param diretorio               pasta das capas ({@code CAPAS_DIR})
 * @param ladoMaximo              maior lado da capa guardada, em px
 * @param larguraMiniatura        largura da miniatura das listas, em px
 * @param tamanhoMaximoDownload   limite de bytes ao baixar a capa de uma fonte
 * @param permitirEnderecosLocais só em testes: deixa baixar de localhost (WireMock)
 */
@ConfigurationProperties("biblioteca.capas")
public record CapaProperties(
        @DefaultValue("/dados/capas") Path diretorio,
        @DefaultValue("800") int ladoMaximo,
        @DefaultValue("200") int larguraMiniatura,
        @DefaultValue("5MB") DataSize tamanhoMaximoDownload,
        @DefaultValue("4s") Duration timeout,
        @DefaultValue("false") boolean permitirEnderecosLocais) {
}
