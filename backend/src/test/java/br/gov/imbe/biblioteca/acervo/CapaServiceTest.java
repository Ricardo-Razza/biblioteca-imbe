package br.gov.imbe.biblioteca.acervo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import br.gov.imbe.biblioteca.isbn.DadosLivro;
import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import br.gov.imbe.biblioteca.isbn.Isbn;
import br.gov.imbe.biblioteca.isbn.IsbnLookupService;
import br.gov.imbe.biblioteca.isbn.ResultadoIsbn;

/** Caminho da capa indicada pela fonte, sem banco: repositório e busca de ISBN simulados. */
class CapaServiceTest {

    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Isbn ISBN = Isbn.de("9788535902778");

    @TempDir
    Path pasta;

    ObraRepository obras = mock(ObraRepository.class);
    IsbnLookupService isbn = mock(IsbnLookupService.class);
    CapaService servico;
    Obra obra;

    @BeforeEach
    void montar() {
        obra = new Obra(ISBN.valor(), "Ensaio sobre a cegueira");
        ReflectionTestUtils.setField(obra, "id", 7L);
        given(obras.findById(7L)).willReturn(Optional.of(obra));
        var propriedades = new CapaProperties(pasta, 800, 200, DataSize.ofMegabytes(5), Duration.ofSeconds(2), true);
        servico = new CapaService(obras, isbn, new TransactionTemplate(mock(PlatformTransactionManager.class)),
                propriedades);
    }

    private void fonteIndicaCapa(String caminho) {
        DadosLivro dados = new DadosLivro(ISBN.valor(), "Ensaio sobre a cegueira", null, List.of(), null, null, null,
                null, null, List.of(), api.baseUrl() + caminho, FonteIsbn.GOOGLE_BOOKS);
        given(isbn.buscar(ISBN)).willReturn(new ResultadoIsbn(ISBN, Optional.of(dados),
                List.of(FonteIsbn.GOOGLE_BOOKS), List.of()));
    }

    private ObraResposta respostaSemCapa(FonteIsbn fonte) {
        ReflectionTestUtils.setField(obra, "fonteDados", fonte);
        return ObraResposta.de(obra);
    }

    @Test
    void baixaEGravaACapaIndicadaPelaFonte() throws Exception {
        fonteIndicaCapa("/capa.jpg");
        api.stubFor(get("/capa.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg")
                .withBody(Imagens.jpeg(400, 600))));

        ObraResposta resposta = servico.tentarCapaDaFonte(respostaSemCapa(FonteIsbn.GOOGLE_BOOKS));

        assertThat(resposta.temCapa()).isTrue();
        assertThat(obra.getCapaArquivo()).startsWith("obra-7-").endsWith(".jpg");
        assertThat(pasta.resolve(obra.getCapaArquivo())).exists();
        assertThat(pasta.resolve(obra.getCapaMiniatura())).exists();
        assertThat(servico.arquivo(7L, true)).contains(pasta.toAbsolutePath().normalize().resolve(obra.getCapaMiniatura()));
    }

    @Test
    void previaECadastroUsamOMesmoDownload() {
        fonteIndicaCapa("/capa.jpg");
        api.stubFor(get("/capa.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg")
                .withBody(Imagens.jpeg(400, 600))));

        assertThat(servico.previaExterna(ISBN)).isPresent();
        servico.tentarCapaDaFonte(respostaSemCapa(FonteIsbn.GOOGLE_BOOKS));

        api.verify(1, getRequestedFor(urlEqualTo("/capa.jpg")));
    }

    @Test
    void falhaNoDownloadDeixaSemCapaENaoFicaNoCache() {
        fonteIndicaCapa("/capa.jpg");
        api.stubFor(get("/capa.jpg").willReturn(serverError()));

        ObraResposta resposta = servico.tentarCapaDaFonte(respostaSemCapa(FonteIsbn.GOOGLE_BOOKS));
        servico.previaExterna(ISBN);

        assertThat(resposta.temCapa()).isFalse();
        assertThat(obra.getCapaArquivo()).isNull();
        api.verify(2, getRequestedFor(urlEqualTo("/capa.jpg")));
    }

    @Test
    void capaIlegivelDaFonteDeixaSemCapa() {
        fonteIndicaCapa("/capa.jpg");
        api.stubFor(get("/capa.jpg").willReturn(aResponse().withHeader("Content-Type", "image/jpeg")
                .withBody("não é jpeg")));

        assertThat(servico.tentarCapaDaFonte(respostaSemCapa(FonteIsbn.GOOGLE_BOOKS)).temCapa()).isFalse();
    }

    @Test
    void cadastroManualNaoBuscaCapa() throws Exception {
        servico.tentarCapaDaFonte(respostaSemCapa(FonteIsbn.MANUAL));

        try (var arquivos = Files.list(pasta)) {
            assertThat(arquivos).isEmpty();
        }
        org.mockito.Mockito.verifyNoInteractions(isbn);
    }
}
