package br.gov.imbe.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.TesteIntegracao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class ObraIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager em;

    private static final String SARAMAGO = """
            {
              "isbn": "978-85-359-0277-8",
              "titulo": "  Ensaio sobre a cegueira ",
              "autores": ["José Saramago", "JOSE SARAMAGO", " "],
              "editora": "Companhia das Letras",
              "ano": 1995,
              "paginas": 312,
              "idioma": "PT",
              "sinopse": "Uma epidemia de cegueira branca.",
              "assuntos": ["Ficção", "Romance", "ficcao"],
              "fonteDados": "BRASILAPI"
            }
            """;

    private ResultActions cadastrar(String json) throws Exception {
        return mvc.perform(post("/api/v1/obras").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long cadastrarERetornarId(String json) throws Exception {
        String corpo = cadastrar(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    @Test
    void cadastraNormalizandoOsDados() throws Exception {
        cadastrar(SARAMAGO)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/obras/\\d+")))
                .andExpect(jsonPath("$.isbn13").value("9788535902778"))
                .andExpect(jsonPath("$.titulo").value("Ensaio sobre a cegueira"))
                .andExpect(jsonPath("$.autores.length()").value(1))
                .andExpect(jsonPath("$.autores[0]").value("José Saramago"))
                .andExpect(jsonPath("$.assuntos.length()").value(2))
                .andExpect(jsonPath("$.idioma").value("pt"))
                .andExpect(jsonPath("$.ano").value(1995))
                .andExpect(jsonPath("$.temCapa").value(false))
                .andExpect(jsonPath("$.fonteDados").value("BRASILAPI"))
                .andExpect(jsonPath("$.criadoEm").exists());
    }

    @Test
    void semFonteInformadaFicaManual() throws Exception {
        cadastrar("""
                {"isbn": "0306406152", "titulo": "Livro antigo"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn13").value("9780306406157"))
                .andExpect(jsonPath("$.fonteDados").value("MANUAL"))
                .andExpect(jsonPath("$.autores.length()").value(0));
    }

    @Test
    void reaproveitaAutorEAssuntoSemDiferenciarMaiusculasNemAcentos() throws Exception {
        cadastrarERetornarId(SARAMAGO);
        cadastrar("""
                {"isbn": "9788571640733", "titulo": "Memorial do convento",
                 "autores": ["JOSE SARAMAGO"], "assuntos": ["FICCAO"]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autores[0]").value("José Saramago"))
                .andExpect(jsonPath("$.assuntos[0]").value("Ficção"));

        assertThat(jdbc.queryForObject("select count(*) from autor", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from assunto", Integer.class)).isEqualTo(2);
    }

    @Test
    void isbnRepetidoDa409MesmoEmOutroFormato() throws Exception {
        cadastrarERetornarId(SARAMAGO);

        cadastrar("""
                {"isbn": "8535902775", "titulo": "Outro título"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Já existe uma obra com o ISBN 9788535902778. Adicione um exemplar a ela."));
    }

    @Test
    void validacaoListaOsCamposEmPortugues() throws Exception {
        cadastrar("""
                {"isbn": "", "titulo": "", "paginas": 0, "ano": 99}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'titulo')].mensagem").value("Informe o título."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'isbn')].mensagem").value("Informe o ISBN."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'paginas')].mensagem").value("Deve ser maior que zero."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'ano')].mensagem").value("Ano inválido."));
    }

    @Test
    void isbnInvalidoDa400ComOMotivo() throws Exception {
        cadastrar("""
                {"isbn": "9788535902779", "titulo": "Livro"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ISBN inválido"));
    }

    @Test
    void alteraEReordenaOsAutores() throws Exception {
        long id = cadastrarERetornarId("""
                {"isbn": "9780306406157", "titulo": "Probability", "autores": ["E. T. Jaynes", "G. L. Bretthorst"]}
                """);

        mvc.perform(put("/api/v1/obras/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9780306406157", "titulo": "Probability theory",
                 "autores": ["G. L. Bretthorst", "Novo Autor", "E. T. Jaynes"]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Probability theory"))
                .andExpect(jsonPath("$.autores[0]").value("G. L. Bretthorst"))
                .andExpect(jsonPath("$.autores[1]").value("Novo Autor"))
                .andExpect(jsonPath("$.autores[2]").value("E. T. Jaynes"));
        // Grava agora: prova que reordenar não viola a chave primária de obra_autor
        em.flush();

        mvc.perform(put("/api/v1/obras/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9780306406157", "titulo": "Probability theory", "autores": ["E. T. Jaynes"]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autores.length()").value(1));
        em.flush();

        assertThat(jdbc.queryForObject("select count(*) from obra_autor where obra_id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void alterarParaUmIsbnDeOutraObraDa409() throws Exception {
        cadastrarERetornarId(SARAMAGO);
        long outra = cadastrarERetornarId("""
                {"isbn": "9780306406157", "titulo": "Probability"}
                """);

        mvc.perform(put("/api/v1/obras/" + outra).contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9788535902778", "titulo": "Probability"}
                """))
                .andExpect(status().isConflict());
    }

    @Test
    void obraInexistenteDa404() throws Exception {
        mvc.perform(get("/api/v1/obras/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Obra 999999 não encontrada."));
    }

    @Test
    void buscaPorTituloAutorOuIsbn() throws Exception {
        cadastrarERetornarId(SARAMAGO);
        cadastrarERetornarId("""
                {"isbn": "9780306406157", "titulo": "Probability theory", "autores": ["E. T. Jaynes"]}
                """);

        mvc.perform(get("/api/v1/obras").param("q", "CEGUEIRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].isbn13").value("9788535902778"));

        mvc.perform(get("/api/v1/obras").param("q", "jose sara"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Ensaio sobre a cegueira"));

        mvc.perform(get("/api/v1/obras").param("q", "0-306-40615-2"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Probability theory"));

        mvc.perform(get("/api/v1/obras").param("q", "100%"))
                .andExpect(jsonPath("$.total").value(0));

        mvc.perform(get("/api/v1/obras"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[0].titulo").value("Ensaio sobre a cegueira"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20));
    }

    @Test
    void ordenacaoPorCampoInexistenteDa400() throws Exception {
        mvc.perform(get("/api/v1/obras").param("sort", "senha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível ordenar por senha."));
    }

    @Test
    void isbnJaCadastradoNaoConsultaAsFontes() throws Exception {
        long id = cadastrarERetornarId(SARAMAGO);

        mvc.perform(get("/api/v1/isbn/8535902775"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obraId").value(id));
    }
}
