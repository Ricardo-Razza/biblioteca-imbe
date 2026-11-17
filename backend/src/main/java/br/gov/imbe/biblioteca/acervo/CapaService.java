package br.gov.imbe.biblioteca.acervo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import br.gov.imbe.biblioteca.acervo.ProcessadorDeImagem.Capa;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.IsbnLookupService;

/**
 * Capas guardadas em disco ({@code CAPAS_DIR}), nunca link externo na tela.
 * <p>
 * Arquivos {@code obra-{id}-{versao}.jpg} e {@code obra-{id}-{versao}-mini.jpg}: a versão muda a cada troca, então
 * o nome do arquivo serve de ETag e a capa antiga é apagada depois que o banco aponta para a nova.
 */
@Service
public class CapaService {

    private static final Logger log = LoggerFactory.getLogger(CapaService.class);

    private final ObraRepository obras;
    private final IsbnLookupService isbn;
    private final TransactionTemplate transacao;
    private final ProcessadorDeImagem processador;
    private final BaixadorDeCapa baixador;
    private final Path diretorio;
    /**
     * Capas externas já baixadas e processadas: a prévia do formulário e o cadastro usam o mesmo download.
     * Só entra o que deu certo; falha de rede não fica guardada.
     */
    private final Cache<String, Capa> externas = Caffeine.newBuilder()
            .maximumSize(100).expireAfterWrite(Duration.ofHours(1)).build();

    public CapaService(ObraRepository obras, IsbnLookupService isbn, TransactionTemplate transacao,
            CapaProperties propriedades) {
        this.obras = obras;
        this.isbn = isbn;
        this.transacao = transacao;
        this.processador = new ProcessadorDeImagem(propriedades.ladoMaximo(), propriedades.larguraMiniatura());
        this.baixador = new BaixadorDeCapa(propriedades);
        this.diretorio = propriedades.diretorio().toAbsolutePath().normalize();
    }

    /** Foto enviada pelo funcionário (formulário manual ou troca de capa). Imagem inválida: 400. */
    public ObraResposta enviar(Long obraId, byte[] imagem) {
        exigirObra(obraId);
        return gravar(obraId, processador.processar(imagem));
    }

    /**
     * Depois do cadastro com dados de uma fonte: busca a capa indicada pela fonte (via cache do ISBN) e grava.
     * Qualquer falha deixa a obra sem capa; o funcionário pode enviar uma foto depois.
     */
    public ObraResposta tentarCapaDaFonte(ObraResposta obra) {
        if (obra.temCapa() || obra.fonteDados() == null || obra.fonteDados() == FonteIsbn.MANUAL) {
            return obra;
        }
        try {
            return previaExterna(Isbn.de(obra.isbn13()))
                    .map(capa -> gravar(obra.id(), capa))
                    .orElse(obra);
        } catch (RuntimeException e) {
            log.warn("Obra {}: capa da fonte não gravada: {}", obra.id(), e.toString());
            return obra;
        }
    }

    /** Capa da fonte para o formulário de conferência, antes de a obra existir. */
    public Optional<Capa> previaExterna(Isbn alvo) {
        return isbn.buscar(alvo).dados()
                .flatMap(d -> Optional.ofNullable(d.capaUrl()))
                .flatMap(this::capaExterna);
    }

    private Optional<Capa> capaExterna(String url) {
        Capa emCache = externas.getIfPresent(url);
        if (emCache != null) {
            return Optional.of(emCache);
        }
        Optional<Capa> capa = baixarEProcessar(url);
        capa.ifPresent(c -> externas.put(url, c));
        return capa;
    }

    /** @return caminho do arquivo da capa, se a obra tiver capa */
    public Optional<Path> arquivo(Long obraId, boolean miniatura) {
        Obra obra = exigirObra(obraId);
        String nome = miniatura ? obra.getCapaMiniatura() : obra.getCapaArquivo();
        if (nome == null) {
            return Optional.empty();
        }
        Path caminho = diretorio.resolve(nome).normalize();
        return caminho.startsWith(diretorio) && Files.isRegularFile(caminho) ? Optional.of(caminho) : Optional.empty();
    }

    private Optional<Capa> baixarEProcessar(String url) {
        return baixador.baixar(url).flatMap(bytes -> {
            try {
                return Optional.of(processador.processar(bytes));
            } catch (RuntimeException e) {
                log.warn("Capa externa ilegível: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    private ObraResposta gravar(Long obraId, Capa capa) {
        String base = "obra-" + obraId + "-" + System.currentTimeMillis();
        String nomeGrande = base + ".jpg";
        String nomeMini = base + "-mini.jpg";
        escrever(nomeGrande, capa.grande());
        escrever(nomeMini, capa.miniatura());

        String[] antigos = new String[2];
        ObraResposta resposta;
        try {
            resposta = transacao.execute(status -> {
                Obra obra = exigirObra(obraId);
                antigos[0] = obra.getCapaArquivo();
                antigos[1] = obra.getCapaMiniatura();
                obra.definirCapa(nomeGrande, nomeMini);
                return ObraResposta.de(obra);
            });
        } catch (RuntimeException e) {
            apagar(nomeGrande);
            apagar(nomeMini);
            throw e;
        }
        apagar(antigos[0]);
        apagar(antigos[1]);
        return resposta;
    }

    /** Grava num temporário e move: quem estiver lendo nunca vê um arquivo pela metade. */
    private void escrever(String nome, byte[] bytes) {
        try {
            Files.createDirectories(diretorio);
            Path temporario = Files.createTempFile(diretorio, ".capa-", ".tmp");
            Files.write(temporario, bytes);
            Files.move(temporario, diretorio.resolve(nome), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a capa em " + diretorio, e);
        }
    }

    private void apagar(String nome) {
        if (nome == null) {
            return;
        }
        try {
            Files.deleteIfExists(diretorio.resolve(nome).normalize());
        } catch (IOException e) {
            log.warn("Capa antiga não apagada: {} ({})", nome, e.toString());
        }
    }

    private Obra exigirObra(Long obraId) {
        return obras.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra " + obraId + " não encontrada."));
    }
}
