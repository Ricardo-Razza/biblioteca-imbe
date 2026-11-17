package br.gov.imbe.biblioteca.acervo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/** Imagens geradas para os testes. */
final class Imagens {

    private Imagens() {
    }

    /** Metade de cima vermelha e de baixo azul: dá para ver para que lado a imagem girou. */
    static BufferedImage duasCores(int largura, int altura, int tipo) {
        BufferedImage img = new BufferedImage(largura, altura, tipo);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, largura, altura / 2);
        g.setColor(Color.BLUE);
        g.fillRect(0, altura / 2, largura, altura - altura / 2);
        g.dispose();
        return img;
    }

    static byte[] codificar(BufferedImage img, String formato) {
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            if (!ImageIO.write(img, formato, saida)) {
                throw new IllegalStateException("Sem codificador para " + formato);
            }
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] jpeg(int largura, int altura) {
        return codificar(duasCores(largura, altura, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    static BufferedImage ler(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new IllegalStateException("Não é imagem");
            }
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Insere um segmento APP1/EXIF mínimo, só com a tag Orientation, logo depois do SOI do JPEG. */
    static byte[] comOrientacaoExif(byte[] jpeg, int orientacao, boolean littleEndian) {
        byte[] tiff = littleEndian
                ? new byte[] { 'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0,
                        (byte) orientacao, 0, 0, 0, 0, 0, 0, 0, 0 }
                : new byte[] { 'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1,
                        0, (byte) orientacao, 0, 0, 0, 0, 0, 0 };
        byte[] cabecalho = { 'E', 'x', 'i', 'f', 0, 0 };
        int tamanho = 2 + cabecalho.length + tiff.length;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.write(jpeg, 0, 2);
        saida.write(0xFF);
        saida.write(0xE1);
        saida.write(tamanho >> 8);
        saida.write(tamanho & 0xFF);
        saida.writeBytes(cabecalho);
        saida.writeBytes(tiff);
        saida.write(jpeg, 2, jpeg.length - 2);
        return saida.toByteArray();
    }

    /** Cor média de uma faixa: "vermelho" ou "azul" com folga para a compressão do JPEG. */
    static boolean ehVermelho(BufferedImage img, int x, int y) {
        Color c = new Color(img.getRGB(x, y));
        return c.getRed() > 150 && c.getBlue() < 100;
    }

    static boolean ehAzul(BufferedImage img, int x, int y) {
        Color c = new Color(img.getRGB(x, y));
        return c.getBlue() > 150 && c.getRed() < 100;
    }

    static ByteArrayInputStream fluxo(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }
}
