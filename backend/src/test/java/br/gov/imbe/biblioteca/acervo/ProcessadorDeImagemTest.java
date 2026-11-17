package br.gov.imbe.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.gov.imbe.biblioteca.acervo.ProcessadorDeImagem.Capa;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

class ProcessadorDeImagemTest {

    private final ProcessadorDeImagem processador = new ProcessadorDeImagem(800, 200);

    @Test
    void reduzFotoGrandeParaOLadoMaximoEGeraMiniatura() {
        Capa capa = processador.processar(Imagens.jpeg(1500, 2400));

        BufferedImage grande = Imagens.ler(capa.grande());
        BufferedImage mini = Imagens.ler(capa.miniatura());
        assertThat(grande.getHeight()).isEqualTo(800);
        assertThat(grande.getWidth()).isEqualTo(500);
        assertThat(mini.getWidth()).isEqualTo(200);
        assertThat(mini.getHeight()).isEqualTo(320);
    }

    @Test
    void naoAmpliaImagemPequena() {
        Capa capa = processador.processar(Imagens.jpeg(128, 190));

        assertThat(Imagens.ler(capa.grande()).getWidth()).isEqualTo(128);
        assertThat(Imagens.ler(capa.miniatura()).getWidth()).isEqualTo(128);
    }

    @Test
    void pngTransparenteViraJpegComFundoBranco() {
        BufferedImage transparente = new BufferedImage(300, 400, BufferedImage.TYPE_INT_ARGB);
        Capa capa = processador.processar(Imagens.codificar(transparente, "png"));

        byte[] grande = capa.grande();
        assertThat(grande[0] & 0xFF).isEqualTo(0xFF);
        assertThat(grande[1] & 0xFF).isEqualTo(0xD8);
        Color pixel = new Color(Imagens.ler(grande).getRGB(150, 200));
        assertThat(pixel.getRed()).isGreaterThan(240);
        assertThat(pixel.getGreen()).isGreaterThan(240);
        assertThat(pixel.getBlue()).isGreaterThan(240);
    }

    @Test
    void aceitaGif() {
        BufferedImage img = Imagens.duasCores(100, 150, BufferedImage.TYPE_INT_RGB);

        assertThat(Imagens.ler(processador.processar(Imagens.codificar(img, "gif")).grande()).getWidth()).isEqualTo(100);
    }

    @Test
    void recusaArquivoQueNaoEImagem() {
        assertThatThrownBy(() -> processador.processar("isto não é uma imagem".getBytes()))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("O arquivo não é uma imagem reconhecida. Use JPG ou PNG.");
    }

    @Test
    void recusaJpegCortado() {
        byte[] jpeg = Imagens.jpeg(400, 600);
        byte[] cortado = java.util.Arrays.copyOf(jpeg, 200);

        assertThatThrownBy(() -> processador.processar(cortado)).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void leAOrientacaoExifNasDuasOrdensDeBytes(boolean littleEndian) {
        byte[] comExif = Imagens.comOrientacaoExif(Imagens.jpeg(40, 60), 6, littleEndian);

        assertThat(ProcessadorDeImagem.orientacaoExif(comExif)).isEqualTo(6);
    }

    @Test
    void semExifOrientacaoEUm() {
        assertThat(ProcessadorDeImagem.orientacaoExif(Imagens.jpeg(40, 60))).isEqualTo(1);
        assertThat(ProcessadorDeImagem.orientacaoExif(new byte[] { 1, 2, 3 })).isEqualTo(1);
        assertThat(ProcessadorDeImagem.orientacaoExif(Imagens.codificar(
                Imagens.duasCores(10, 10, BufferedImage.TYPE_INT_RGB), "png"))).isEqualTo(1);
    }

    @Test
    void fotoDeCelularDeitadaFicaEmPe() {
        // Sensor gravou 600x400 (deitada) com Orientation=6: precisa girar 90° no sentido horário.
        // Antes de girar: vermelho em cima, azul embaixo. Depois: vermelho à direita, azul à esquerda.
        byte[] deitada = Imagens.comOrientacaoExif(Imagens.jpeg(600, 400), 6, false);

        BufferedImage capa = Imagens.ler(processador.processar(deitada).grande());

        assertThat(capa.getWidth()).isEqualTo(400);
        assertThat(capa.getHeight()).isEqualTo(600);
        assertThat(Imagens.ehVermelho(capa, 350, 300)).isTrue();
        assertThat(Imagens.ehAzul(capa, 50, 300)).isTrue();
    }

    @Test
    void orientacao8GiraNoSentidoAntiHorario() {
        BufferedImage original = Imagens.duasCores(60, 40, BufferedImage.TYPE_INT_RGB);

        BufferedImage girada = ProcessadorDeImagem.orientar(original, 8);

        assertThat(girada.getWidth()).isEqualTo(40);
        assertThat(girada.getHeight()).isEqualTo(60);
        assertThat(Imagens.ehVermelho(girada, 5, 30)).isTrue();
        assertThat(Imagens.ehAzul(girada, 35, 30)).isTrue();
    }

    @Test
    void orientacao3GiraMeiaVolta() {
        BufferedImage girada = ProcessadorDeImagem.orientar(Imagens.duasCores(60, 40, BufferedImage.TYPE_INT_RGB), 3);

        assertThat(girada.getWidth()).isEqualTo(60);
        assertThat(Imagens.ehAzul(girada, 30, 5)).isTrue();
        assertThat(Imagens.ehVermelho(girada, 30, 35)).isTrue();
    }
}
