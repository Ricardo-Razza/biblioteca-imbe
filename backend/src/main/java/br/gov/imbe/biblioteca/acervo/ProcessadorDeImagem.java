package br.gov.imbe.biblioteca.acervo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/**
 * Transforma qualquer imagem aceita (JPEG, PNG, GIF, BMP) em duas JPEG: a capa (maior lado até {@code ladoMaximo})
 * e a miniatura ({@code larguraMiniatura} de largura). Regravar em JPEG também descarta os metadados, como o
 * GPS das fotos de celular. A orientação EXIF do celular é aplicada antes, para a capa não ficar deitada.
 */
public class ProcessadorDeImagem {

    /** Proteção contra imagens com dimensões absurdas que estourariam a memória ao decodificar. */
    private static final long MAX_PIXELS = 50_000_000L;
    private static final float QUALIDADE_JPEG = 0.85f;
    private static final String TITULO_ERRO = "Imagem inválida";

    /** As duas versões, já em JPEG. */
    public record Capa(byte[] grande, byte[] miniatura) {
    }

    private final int ladoMaximo;
    private final int larguraMiniatura;

    public ProcessadorDeImagem(int ladoMaximo, int larguraMiniatura) {
        this.ladoMaximo = ladoMaximo;
        this.larguraMiniatura = larguraMiniatura;
    }

    public Capa processar(byte[] original) {
        BufferedImage imagem = ler(original);
        imagem = orientar(imagem, orientacaoExif(original));
        imagem = paraRgb(imagem);
        BufferedImage grande = reduzirAte(imagem, ladoMaximo, ladoMaximo);
        BufferedImage mini = reduzirAte(imagem, larguraMiniatura, Integer.MAX_VALUE);
        return new Capa(jpeg(grande), jpeg(mini));
    }

    private static BufferedImage ler(byte[] bytes) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> leitores = entrada == null ? null : ImageIO.getImageReaders(entrada);
            if (leitores == null || !leitores.hasNext()) {
                throw new RequisicaoInvalidaException(TITULO_ERRO,
                        "O arquivo não é uma imagem reconhecida. Use JPG ou PNG.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new RequisicaoInvalidaException(TITULO_ERRO, "A imagem é grande demais. Use uma foto menor.");
                }
                return leitor.read(0);
            } finally {
                leitor.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof RequisicaoInvalidaException r) {
                throw r;
            }
            throw new RequisicaoInvalidaException(TITULO_ERRO, "Não foi possível ler a imagem. Use JPG ou PNG.");
        }
    }

    /** Fundo branco no lugar da transparência; JPEG não tem canal alfa. */
    private static BufferedImage paraRgb(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) {
            return origem;
        }
        BufferedImage rgb = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(origem, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /** Reduz mantendo a proporção para caber em largura x altura. Nunca amplia. */
    static BufferedImage reduzirAte(BufferedImage origem, int larguraMax, int alturaMax) {
        double fator = Math.min(1.0, Math.min((double) larguraMax / origem.getWidth(),
                (double) alturaMax / origem.getHeight()));
        int largura = Math.max(1, (int) Math.round(origem.getWidth() * fator));
        int altura = Math.max(1, (int) Math.round(origem.getHeight() * fator));
        BufferedImage atual = origem;
        // Reduz pela metade até chegar perto: uma redução grande de uma vez só fica serrilhada
        while (atual.getWidth() / 2 >= largura && atual.getHeight() / 2 >= altura) {
            atual = desenhar(atual, atual.getWidth() / 2, atual.getHeight() / 2);
        }
        return atual.getWidth() == largura && atual.getHeight() == altura ? atual : desenhar(atual, largura, altura);
    }

    private static BufferedImage desenhar(BufferedImage origem, int largura, int altura) {
        BufferedImage destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origem, 0, 0, largura, altura, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    private static byte[] jpeg(BufferedImage imagem) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(saida)) {
            escritor.setOutput(out);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(QUALIDADE_JPEG);
            escritor.write(null, new IIOImage(imagem, null, null), parametros);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar JPEG", e);
        } finally {
            escritor.dispose();
        }
        return saida.toByteArray();
    }

    /** Aplica a orientação EXIF (1 a 8). 1 ou desconhecida: devolve como está. */
    static BufferedImage orientar(BufferedImage img, int orientacao) {
        if (orientacao < 2 || orientacao > 8) {
            return img;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        boolean troca = orientacao >= 5;
        AffineTransform t = new AffineTransform();
        switch (orientacao) {
            case 2 -> { t.scale(-1, 1); t.translate(-w, 0); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.scale(1, -1); t.translate(0, -h); }
            case 5 -> { t.rotate(-Math.PI / 2); t.scale(-1, 1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { }
        }
        int tipo = img.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : img.getType();
        BufferedImage destino = new BufferedImage(troca ? h : w, troca ? w : h, tipo);
        Graphics2D g = destino.createGraphics();
        try {
            g.drawImage(img, t, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    /**
     * Lê a tag Orientation (0x0112) do EXIF de um JPEG. Leitura mínima e defensiva: qualquer coisa fora do
     * esperado devolve 1 (sem rotação).
     */
    static int orientacaoExif(byte[] b) {
        if (b.length < 4 || u8(b, 0) != 0xFF || u8(b, 1) != 0xD8) {
            return 1;
        }
        int i = 2;
        while (i + 4 <= b.length && u8(b, i) == 0xFF) {
            int marcador = u8(b, i + 1);
            if (marcador == 0xDA || marcador == 0xD9) {
                return 1;
            }
            int tamanho = (u8(b, i + 2) << 8) | u8(b, i + 3);
            int fim = Math.min(b.length, i + 2 + tamanho);
            if (marcador == 0xE1 && i + 10 <= fim && b[i + 4] == 'E' && b[i + 5] == 'x' && b[i + 6] == 'i'
                    && b[i + 7] == 'f' && b[i + 8] == 0 && b[i + 9] == 0) {
                return lerOrientacaoTiff(b, i + 10, fim);
            }
            if (tamanho < 2) {
                return 1;
            }
            i += 2 + tamanho;
        }
        return 1;
    }

    private static int lerOrientacaoTiff(byte[] b, int inicio, int fim) {
        if (inicio + 8 > fim) {
            return 1;
        }
        boolean le = b[inicio] == 'I' && b[inicio + 1] == 'I';
        if (!le && !(b[inicio] == 'M' && b[inicio + 1] == 'M')) {
            return 1;
        }
        if (u16(b, inicio + 2, le) != 42) {
            return 1;
        }
        long deslocamento = u32(b, inicio + 4, le);
        if (deslocamento < 8 || inicio + deslocamento + 2 > fim) {
            return 1;
        }
        int ifd = (int) (inicio + deslocamento);
        int entradas = u16(b, ifd, le);
        for (int k = 0; k < entradas; k++) {
            int e = ifd + 2 + 12 * k;
            if (e + 12 > fim) {
                return 1;
            }
            if (u16(b, e, le) == 0x0112) {
                int valor = u16(b, e + 8, le);
                return valor >= 1 && valor <= 8 ? valor : 1;
            }
        }
        return 1;
    }

    private static int u8(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    private static int u16(byte[] b, int i, boolean le) {
        return le ? u8(b, i) | (u8(b, i + 1) << 8) : (u8(b, i) << 8) | u8(b, i + 1);
    }

    private static long u32(byte[] b, int i, boolean le) {
        return le ? (u16(b, i, true) & 0xFFFFL) | ((long) u16(b, i + 2, true) << 16)
                : ((long) u16(b, i, false) << 16) | (u16(b, i + 2, false) & 0xFFFFL);
    }
}
