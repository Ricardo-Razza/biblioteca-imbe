package br.gov.imbe.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.TesteIntegracao;

@TesteIntegracao
@Transactional
class CapaIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CapaProperties propriedades;

    long obraId;

    @BeforeEach
    void cadastrarObra() throws Exception {
        String corpo = mvc.perform(post("/api/v1/obras").contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9788535902778", "titulo": "Ensaio sobre a cegueira"}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        obraId = ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private MvcResult enviar(byte[] imagem) throws Exception {
        return mvc.perform(multipart("/api/v1/obras/{id}/capa", obraId)
                .file(new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", imagem)))
                .andReturn();
    }

    @Test
    void obraSemCapaDa404() throws Exception {
        mvc.perform(get("/api/v1/obras/{id}/capa", obraId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/obras/{id}", obraId)).andExpect(jsonPath("$.temCapa").value(false));
    }

    @Test
    void enviaFotoEServeGrandeEMiniatura() throws Exception {
        MvcResult envio = enviar(Imagens.jpeg(1200, 1800));
        assertThat(envio.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(envio.getResponse().getContentAsString(), "$.temCapa")).isTrue();

        byte[] grande = mvc.perform(get("/api/v1/obras/{id}/capa", obraId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] mini = mvc.perform(get("/api/v1/obras/{id}/capa", obraId).param("tamanho", "mini"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage g = Imagens.ler(grande);
        BufferedImage m = Imagens.ler(mini);
        assertThat(g.getHeight()).isEqualTo(800);
        assertThat(m.getWidth()).isEqualTo(200);
    }

    @Test
    void etagIgualDevolve304() throws Exception {
        enviar(Imagens.jpeg(300, 450));
        String etag = mvc.perform(get("/api/v1/obras/{id}/capa", obraId))
                .andReturn().getResponse().getHeader("ETag");

        mvc.perform(get("/api/v1/obras/{id}/capa", obraId).header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void trocarACapaApagaOsArquivosAntigos() throws Exception {
        enviar(Imagens.jpeg(300, 450));
        String etagAntigo = mvc.perform(get("/api/v1/obras/{id}/capa", obraId)).andReturn().getResponse().getHeader("ETag");
        Path antigo = propriedades.diretorio().resolve(etagAntigo.replace("\"", ""));
        assertThat(antigo).exists();

        Thread.sleep(5); // a versão do arquivo é o instante em ms
        enviar(Imagens.jpeg(320, 480));

        String etagNovo = mvc.perform(get("/api/v1/obras/{id}/capa", obraId)).andReturn().getResponse().getHeader("ETag");
        assertThat(etagNovo).isNotEqualTo(etagAntigo);
        assertThat(antigo).doesNotExist();
        assertThat(Path.of(antigo.toString().replace(".jpg", "-mini.jpg"))).doesNotExist();
    }

    @Test
    void arquivoQueNaoEImagemDa400() throws Exception {
        mvc.perform(multipart("/api/v1/obras/{id}/capa", obraId)
                .file(new MockMultipartFile("arquivo", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Imagem inválida"));
    }

    @Test
    void semOArquivoDa400() throws Exception {
        mvc.perform(multipart("/api/v1/obras/{id}/capa", obraId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Envie o arquivo no campo arquivo."));
    }

    @Test
    void tamanhoInvalidoDa400() throws Exception {
        mvc.perform(get("/api/v1/obras/{id}/capa", obraId).param("tamanho", "enorme"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obraInexistenteDa404() throws Exception {
        mvc.perform(multipart("/api/v1/obras/{id}/capa", 999999)
                .file(new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", Imagens.jpeg(10, 10))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/obras/{id}/capa", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void cadastroComFonteForaDoArFicaSemCapa() throws Exception {
        // Nos testes as fontes apontam para uma porta fechada: o cadastro funciona, só sem capa
        mvc.perform(post("/api/v1/obras").contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9780306406157", "titulo": "Probability", "fonteDados": "OPEN_LIBRARY"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.temCapa").value(false));
    }

    @Test
    void previaPorIsbnSemCapaDa404() throws Exception {
        mvc.perform(get("/api/v1/isbn/9780306406157/capa")).andExpect(status().isNotFound());
    }

    @Test
    void nomeDoArquivoNaoEscapaDaPasta() throws Exception {
        enviar(Imagens.jpeg(100, 150));
        try (var arquivos = Files.list(propriedades.diretorio())) {
            assertThat(arquivos.map(p -> p.getFileName().toString()))
                    .allMatch(n -> n.startsWith("obra-") || n.startsWith(".capa-"));
        }
    }
}
