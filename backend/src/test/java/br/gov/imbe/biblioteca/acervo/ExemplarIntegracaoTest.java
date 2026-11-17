package br.gov.imbe.biblioteca.acervo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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
class ExemplarIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager em;

    long obraId;

    @BeforeEach
    void cadastrarObra() throws Exception {
        String corpo = mvc.perform(post("/api/v1/obras").contentType(MediaType.APPLICATION_JSON).content("""
                {"isbn": "9788535902778", "titulo": "Ensaio sobre a cegueira", "autores": ["José Saramago"]}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        obraId = ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions cadastrar(String json) throws Exception {
        return mvc.perform(post("/api/v1/obras/" + obraId + "/exemplares")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long cadastrarERetornarId(String tombo) throws Exception {
        String corpo = cadastrar("{\"tombo\": \"" + tombo + "\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions alterar(long id, String json) throws Exception {
        return mvc.perform(patch("/api/v1/exemplares/" + id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Os contadores da obra são {@code @Formula}: só refletem o banco ao recarregar a entidade. */
    private void recarregar() {
        em.flush();
        em.clear();
    }

    @Test
    void cadastraComPadroes() throws Exception {
        cadastrar("""
                {"tombo": " 2026-0001 ", "localizacao": " Estante 3 ", "dataAquisicao": "2026-03-10",
                 "origem": "DOACAO"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern("/api/v1/exemplares/\\d+")))
                .andExpect(jsonPath("$.tombo").value("2026-0001"))
                .andExpect(jsonPath("$.localizacao").value("Estante 3"))
                .andExpect(jsonPath("$.estado").value("BOM"))
                .andExpect(jsonPath("$.situacao").value("DISPONIVEL"))
                .andExpect(jsonPath("$.origem").value("DOACAO"))
                .andExpect(jsonPath("$.dataAquisicao").value("2026-03-10"))
                .andExpect(jsonPath("$.obra.id").value(obraId))
                .andExpect(jsonPath("$.obra.titulo").value("Ensaio sobre a cegueira"))
                .andExpect(jsonPath("$.obra.autores[0]").value("José Saramago"));
    }

    @Test
    void tomboRepetidoDa409MesmoComOutraCaixa() throws Exception {
        cadastrarERetornarId("ab-10");
        cadastrar("{\"tombo\": \"AB-10\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um exemplar com o tombo AB-10."));
    }

    @Test
    void tomboComBarraOuEspacoDa400() throws Exception {
        cadastrar("{\"tombo\": \"12/2026\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("tombo"));
        cadastrar("{\"tombo\": \"12 34\"}").andExpect(status().isBadRequest());
        cadastrar("{\"tombo\": \"  \"}").andExpect(status().isBadRequest());
    }

    @Test
    void dataFuturaDa400() throws Exception {
        cadastrar("{\"tombo\": \"X1\", \"dataAquisicao\": \"" + LocalDate.now().plusDays(2) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("dataAquisicao"));
    }

    @Test
    void valorDeEnumInvalidoDa400() throws Exception {
        cadastrar("{\"tombo\": \"X1\", \"origem\": \"ROUBO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void obraInexistenteDa404() throws Exception {
        mvc.perform(post("/api/v1/obras/999999/exemplares").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tombo\": \"X1\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/obras/999999/exemplares")).andExpect(status().isNotFound());
    }

    @Test
    void buscaPorTomboSemDiferenciarCaixa() throws Exception {
        cadastrarERetornarId("BIB.0042");
        mvc.perform(get("/api/v1/exemplares/tombo/bib.0042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tombo").value("BIB.0042"))
                .andExpect(jsonPath("$.obra.isbn13").value("9788535902778"));
        mvc.perform(get("/api/v1/exemplares/tombo/NAO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Nenhum exemplar com o tombo NAO-EXISTE."));
    }

    @Test
    void listaDaObraEmOrdemDeTombo() throws Exception {
        cadastrarERetornarId("B2");
        cadastrarERetornarId("A1");
        mvc.perform(get("/api/v1/obras/" + obraId + "/exemplares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tombo").value("A1"))
                .andExpect(jsonPath("$[1].tombo").value("B2"));
    }

    @Test
    void obraContaExemplaresSemOsBaixados() throws Exception {
        mvc.perform(get("/api/v1/obras/" + obraId))
                .andExpect(jsonPath("$.totalExemplares").value(0))
                .andExpect(jsonPath("$.exemplaresDisponiveis").value(0));

        cadastrarERetornarId("C1");
        long extraviado = cadastrarERetornarId("C2");
        long baixado = cadastrarERetornarId("C3");
        alterar(extraviado, "{\"situacao\": \"EXTRAVIADO\"}").andExpect(status().isOk());
        alterar(baixado, "{\"situacao\": \"BAIXADO\"}").andExpect(status().isOk());
        recarregar();

        mvc.perform(get("/api/v1/obras/" + obraId))
                .andExpect(jsonPath("$.totalExemplares").value(2))
                .andExpect(jsonPath("$.exemplaresDisponiveis").value(1));
        mvc.perform(get("/api/v1/obras").param("q", "cegueira"))
                .andExpect(jsonPath("$.itens[0].totalExemplares").value(2))
                .andExpect(jsonPath("$.itens[0].exemplaresDisponiveis").value(1));
    }

    @Test
    void extraviadoVoltaEBaixadoEDefinitivo() throws Exception {
        long id = cadastrarERetornarId("D1");
        alterar(id, "{\"situacao\": \"EXTRAVIADO\"}").andExpect(jsonPath("$.situacao").value("EXTRAVIADO"));
        alterar(id, "{\"situacao\": \"DISPONIVEL\"}").andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        alterar(id, "{\"situacao\": \"BAIXADO\"}").andExpect(jsonPath("$.situacao").value("BAIXADO"));

        alterar(id, "{\"situacao\": \"DISPONIVEL\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Exemplar D1 está baixado e não volta ao acervo."));
        alterar(id, "{\"localizacao\": \"Depósito\"}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void disponivelNaoVaiParaDisponivelPorOutroCaminhoNemParaEmprestado() throws Exception {
        long id = cadastrarERetornarId("E1");
        alterar(id, "{\"situacao\": \"EMPRESTADO\"}").andExpect(status().isUnprocessableContent());
        alterar(id, "{\"situacao\": \"RESERVADO\"}").andExpect(status().isUnprocessableContent());
        alterar(id, "{\"situacao\": \"DISPONIVEL\"}").andExpect(status().isOk());
    }

    @Test
    void emprestadoNaoMudaDeSituacaoMasMudaDeEstante() throws Exception {
        long id = cadastrarERetornarId("F1");
        recarregar();
        jdbc.update("update exemplar set situacao = 'EMPRESTADO' where id = ?", id);

        alterar(id, "{\"situacao\": \"EXTRAVIADO\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("está emprestado")));
        alterar(id, "{\"localizacao\": \"Estante 7\", \"estado\": \"DANIFICADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localizacao").value("Estante 7"))
                .andExpect(jsonPath("$.estado").value("DANIFICADO"))
                .andExpect(jsonPath("$.situacao").value("EMPRESTADO"));
    }

    @Test
    void localizacaoEmBrancoLimpaEAusenteMantem() throws Exception {
        String corpo = cadastrar("{\"tombo\": \"G1\", \"localizacao\": \"Estante 1\"}")
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        alterar(id, "{\"estado\": \"DANIFICADO\"}").andExpect(jsonPath("$.localizacao").value("Estante 1"));
        alterar(id, "{\"localizacao\": \"\"}").andExpect(jsonPath("$.localizacao").doesNotExist());
    }

    @Test
    void exemplarInexistenteDa404() throws Exception {
        alterar(999999, "{\"situacao\": \"BAIXADO\"}").andExpect(status().isNotFound());
    }
}
