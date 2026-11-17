package br.gov.imbe.biblioteca.acervo;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baixa a imagem de capa indicada por uma fonte externa.
 * <p>
 * A URL vem de terceiros, então: só http/https, nada de endereço interno da rede (nem por redirecionamento,
 * que é seguido à mão e conferido a cada salto), limite de bytes e tipo {@code image/*}.
 */
public class BaixadorDeCapa {

    private static final Logger log = LoggerFactory.getLogger(BaixadorDeCapa.class);
    private static final int MAX_REDIRECIONAMENTOS = 3;
    private static final Set<Integer> REDIRECIONAMENTOS = Set.of(301, 302, 303, 307, 308);

    private final HttpClient http;
    private final CapaProperties propriedades;

    public BaixadorDeCapa(CapaProperties propriedades) {
        this.propriedades = propriedades;
        this.http = HttpClient.newBuilder()
                .connectTimeout(propriedades.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** @return os bytes, ou vazio se não houver imagem (404, tipo errado, endereço recusado, falha de rede) */
    public Optional<byte[]> baixar(String url) {
        try {
            URI uri = URI.create(url);
            for (int salto = 0; salto <= MAX_REDIRECIONAMENTOS; salto++) {
                if (!permitido(uri)) {
                    log.warn("Capa recusada: endereço não permitido ({})", uri.getHost());
                    return Optional.empty();
                }
                HttpRequest requisicao = HttpRequest.newBuilder(uri)
                        .timeout(propriedades.timeout())
                        .header("User-Agent", "BibliotecaMunicipalImbe/1.0")
                        .header("Accept", "image/*")
                        .GET()
                        .build();
                HttpResponse<InputStream> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream corpo = resposta.body()) {
                    int status = resposta.statusCode();
                    if (REDIRECIONAMENTOS.contains(status)) {
                        Optional<String> destino = resposta.headers().firstValue("Location");
                        if (destino.isEmpty()) {
                            return Optional.empty();
                        }
                        uri = uri.resolve(destino.get());
                        continue;
                    }
                    if (status != 200) {
                        log.debug("Capa não disponível em {}: HTTP {}", uri.getHost(), status);
                        return Optional.empty();
                    }
                    String tipo = resposta.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                    if (!tipo.startsWith("image/")) {
                        log.warn("Capa recusada: tipo {} em {}", tipo, uri.getHost());
                        return Optional.empty();
                    }
                    return lerComLimite(corpo, uri);
                }
            }
            log.warn("Capa recusada: redirecionamentos demais");
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Falha ao baixar capa: {}", e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<byte[]> lerComLimite(InputStream corpo, URI uri) throws IOException {
        long limite = propriedades.tamanhoMaximoDownload().toBytes();
        byte[] bytes = corpo.readNBytes((int) limite + 1);
        if (bytes.length > limite) {
            log.warn("Capa recusada: passa de {} bytes ({})", limite, uri.getHost());
            return Optional.empty();
        }
        return bytes.length == 0 ? Optional.empty() : Optional.of(bytes);
    }

    private boolean permitido(URI uri) throws IOException {
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("https") && !esquema.equals("http") || uri.getHost() == null) {
            return false;
        }
        if (propriedades.permitirEnderecosLocais()) {
            return true;
        }
        for (InetAddress endereco : InetAddress.getAllByName(uri.getHost())) {
            if (interno(endereco)) {
                return false;
            }
        }
        return true;
    }

    /** Loopback, rede privada, link-local, multicast ou IPv6 de uso local (fc00::/7). */
    static boolean interno(InetAddress endereco) {
        if (endereco.isLoopbackAddress() || endereco.isSiteLocalAddress() || endereco.isLinkLocalAddress()
                || endereco.isAnyLocalAddress() || endereco.isMulticastAddress()) {
            return true;
        }
        if (endereco instanceof Inet6Address) {
            return (endereco.getAddress()[0] & 0xFE) == 0xFC;
        }
        byte[] ip = endereco.getAddress();
        // 100.64.0.0/10 (CGNAT) e 169.254.0.0/16 já coberto por link-local
        return (ip[0] & 0xFF) == 100 && (ip[1] & 0xC0) == 64;
    }
}
