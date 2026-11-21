package br.gov.imbe.biblioteca.emprestimo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
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

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.TesteIntegracao;
import br.gov.imbe.biblioteca.comum.config.RelogioConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class EmprestimoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager em;

    long leitor;
    long obra;
    final LocalDate hoje = LocalDate.now(RelogioConfig.FUSO);

    @BeforeEach
    void preparar() throws Exception {
        leitor = Dados.leitor(mvc, "Carla Leitora");
        obra = Dados.obra(mvc, Dados.isbn(700), "Memórias Póstumas");
        for (int i = 1; i <= 5; i++) {
            Dados.exemplar(mvc, obra, "E-" + i);
        }
    }

    private ResultActions emprestar(long leitorId, String tombo) throws Exception {
        return mvc.perform(post("/api/v1/emprestimos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"leitorId\": " + leitorId + ", \"tombo\": \"" + tombo + "\"}"));
    }

    private long emprestarERetornarId(String tombo) throws Exception {
        String corpo = emprestar(leitor, tombo).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions devolver(long id, String json) throws Exception {
        return mvc.perform(post("/api/v1/emprestimos/" + id + "/devolucao").contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Volta o empréstimo no tempo: saiu há {@code dias} e venceu há {@code atraso}. */
    private void atrasar(long id, int atraso) {
        em.flush();
        em.clear();
        jdbc.update("update emprestimo set data_saida = ?, data_prevista = ? where id = ?",
                hoje.minusDays(atraso + 14), hoje.minusDays(atraso), id);
    }

    @Test
    void emprestaComPrazoEmDiaUtilEMarcaOExemplar() throws Exception {
        String corpo = emprestar(leitor, "e-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.exemplar.tombo").value("E-1"))
                .andExpect(jsonPath("$.exemplar.titulo").value("Memórias Póstumas"))
                .andExpect(jsonPath("$.leitor.nome").value("Carla Leitora"))
                .andExpect(jsonPath("$.dataSaida").value(hoje.toString()))
                .andExpect(jsonPath("$.renovacoesRestantes").value(2))
                .andReturn().getResponse().getContentAsString();
        LocalDate prevista = LocalDate.parse(JsonPath.read(corpo, "$.dataPrevista"));
        assertThat(prevista).isAfterOrEqualTo(hoje.plusDays(14));
        assertThat(prevista.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        mvc.perform(get("/api/v1/exemplares/tombo/E-1")).andExpect(jsonPath("$.situacao").value("EMPRESTADO"));
        emprestar(Dados.leitor(mvc, "Outro"), "E-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("O exemplar E-1 está emprestado e não pode ser emprestado."));
    }

    @Test
    void prazoQueCaiEmFeriadoPassaParaODiaUtilSeguinte() throws Exception {
        LocalDate alvo = hoje.plusDays(14);
        while (alvo.getDayOfWeek() == DayOfWeek.SATURDAY || alvo.getDayOfWeek() == DayOfWeek.SUNDAY) {
            alvo = alvo.plusDays(1);
        }
        jdbc.update("insert into feriado (data, descricao, tipo) values (?, 'Feriado de teste', 'MUNICIPAL')", alvo);
        String corpo = emprestar(leitor, "E-1").andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        assertThat(LocalDate.parse(JsonPath.read(corpo, "$.dataPrevista"))).isAfter(alvo);
    }

    @Test
    void limiteDeLivros() throws Exception {
        emprestarERetornarId("E-1");
        emprestarERetornarId("E-2");
        emprestarERetornarId("E-3");
        emprestar(leitor, "E-4")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Já está com 3 de 3 livros permitidos."));
        mvc.perform(get("/api/v1/leitores/" + leitor + "/situacao"))
                .andExpect(jsonPath("$.podeEmprestar").value(false))
                .andExpect(jsonPath("$.emprestimosEmAberto").value(3));
    }

    @Test
    void limiteSegueOParametro() throws Exception {
        mvc.perform(put("/api/v1/parametros/LIMITE_LIVROS_POR_LEITOR").contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 1}")).andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(1));
        emprestarERetornarId("E-1");
        emprestar(leitor, "E-2").andExpect(status().isUnprocessableContent());
        mvc.perform(put("/api/v1/parametros/LIMITE_LIVROS_POR_LEITOR").contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 99}")).andExpect(status().isBadRequest());
    }

    @Test
    void leitorComAtrasoNaoPegaOutroNemRenova() throws Exception {
        long id = emprestarERetornarId("E-1");
        atrasar(id, 3);
        mvc.perform(get("/api/v1/leitores/" + leitor + "/situacao"))
                .andExpect(jsonPath("$.atrasados").value(1))
                .andExpect(jsonPath("$.motivos[0]").value("Tem 1 livro em atraso."));
        emprestar(leitor, "E-2").andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/emprestimos/" + id + "/renovacao"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", Matchers.containsString("atrasado")));
        mvc.perform(get("/api/v1/emprestimos").param("status", "ATRASADO"))
                .andExpect(jsonPath("$.itens[*].id", Matchers.hasItem((int) id)))
                .andExpect(jsonPath("$.itens[0].diasAtraso").value(3));
    }

    @Test
    void renovaAteOMaximo() throws Exception {
        long id = emprestarERetornarId("E-1");
        mvc.perform(post("/api/v1/emprestimos/" + id + "/renovacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renovacoes").value(1))
                .andExpect(jsonPath("$.renovacoesRestantes").value(1));
        mvc.perform(post("/api/v1/emprestimos/" + id + "/renovacao")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/emprestimos/" + id + "/renovacao"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Este empréstimo já foi renovado 2 vezes, o máximo permitido."));
    }

    @Test
    void devolucaoNoPrazoLiberaOExemplar() throws Exception {
        long id = emprestarERetornarId("E-1");
        devolver(id, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVOLVIDO"))
                .andExpect(jsonPath("$.dataDevolucao").value(hoje.toString()))
                .andExpect(jsonPath("$.bloqueadoAte").doesNotExist());
        mvc.perform(get("/api/v1/exemplares/tombo/E-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        devolver(id, "{}").andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/emprestimos").param("status", "DEVOLVIDO").param("leitorId", String.valueOf(leitor)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void devolucaoAtrasadaBloqueiaPelosDiasDeAtraso() throws Exception {
        long id = emprestarERetornarId("E-1");
        atrasar(id, 3);
        devolver(id, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasAtraso").value(3))
                .andExpect(jsonPath("$.bloqueadoAte").value(hoje.plusDays(2).toString()));
        mvc.perform(get("/api/v1/leitores/" + leitor + "/situacao"))
                .andExpect(jsonPath("$.podeEmprestar").value(false))
                .andExpect(jsonPath("$.motivos[0]", Matchers.startsWith("Leitor bloqueado até")));
    }

    @Test
    void devolucaoComOcorrencia() throws Exception {
        long danificado = emprestarERetornarId("E-1");
        long perdido = emprestarERetornarId("E-2");
        devolver(danificado, "{\"ocorrencia\": \"DANIFICADO\", \"observacao\": \"Capa rasgada\"}")
                .andExpect(jsonPath("$.ocorrenciaDevolucao").value("DANIFICADO"))
                .andExpect(jsonPath("$.observacao").value("Capa rasgada"));
        devolver(perdido, "{\"ocorrencia\": \"EXTRAVIADO\"}").andExpect(status().isOk());
        mvc.perform(get("/api/v1/exemplares/tombo/E-1"))
                .andExpect(jsonPath("$.situacao").value("DISPONIVEL"))
                .andExpect(jsonPath("$.estado").value("DANIFICADO"));
        mvc.perform(get("/api/v1/exemplares/tombo/E-2")).andExpect(jsonPath("$.situacao").value("EXTRAVIADO"));
    }

    @Test
    void buscaPorTomboParaADevolucao() throws Exception {
        long id = emprestarERetornarId("E-3");
        mvc.perform(get("/api/v1/emprestimos").param("tombo", "e-3"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id));
        mvc.perform(get("/api/v1/emprestimos").param("q", "póstumas"))
                .andExpect(jsonPath("$.itens[*].id", Matchers.hasItem((int) id)));
        // Livro sem etiqueta: também pelo autor, pelo leitor ou por um trecho do tombo
        for (String termo : new String[] { "autor de teste", "carla leitora", "e-3" }) {
            mvc.perform(get("/api/v1/emprestimos").param("q", termo))
                    .andExpect(jsonPath("$.itens[*].id", Matchers.hasItem((int) id)));
        }
        mvc.perform(get("/api/v1/emprestimos").param("q", "nada parecido com isso"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void leitorDesativadoOuBloqueadoNaoPega() throws Exception {
        em.flush();
        em.clear();
        jdbc.update("update leitor set bloqueado_ate = ? where id = ?", hoje, leitor);
        emprestar(leitor, "E-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", Matchers.startsWith("Leitor bloqueado até")));
        em.clear();
        jdbc.update("update leitor set bloqueado_ate = ?, ativo = false where id = ?", hoje.minusDays(1), leitor);
        emprestar(leitor, "E-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Leitor desativado."));
    }

    @Test
    void tomboInexistente() throws Exception {
        emprestar(leitor, "NAO-EXISTE").andExpect(status().isNotFound());
    }

    @Test
    void exemplarEmprestadoNaoMudaPelaGestao() throws Exception {
        emprestarERetornarId("E-1");
        String exemplar = mvc.perform(get("/api/v1/exemplares/tombo/E-1")).andReturn().getResponse()
                .getContentAsString();
        int exemplarId = JsonPath.read(exemplar, "$.id");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/exemplares/" + exemplarId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\": \"BAIXADO\"}")).andExpect(status().isUnprocessableContent());
    }
}
