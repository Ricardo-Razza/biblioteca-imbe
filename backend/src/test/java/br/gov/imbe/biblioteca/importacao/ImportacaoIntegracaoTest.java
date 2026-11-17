package br.gov.imbe.biblioteca.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.TesteIntegracao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * As fontes de ISBN apontam para uma porta fechada nos testes: ISBN que não está no acervo sai como
 * "fonte indisponível". O caminho com dados da fonte é coberto em {@code ObraDaFonteTest}.
 */
@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class ImportacaoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ProcessadorDeImportacao processador;

    @PersistenceContext
    EntityManager em;

    String isbnNoAcervo;
    long obra;

    @BeforeEach
    void preparar() throws Exception {
        isbnNoAcervo = Dados.isbn(901);
        obra = Dados.obra(mvc, isbnNoAcervo, "Quarto de Despejo");
        Dados.exemplar(mvc, obra, "IMP-1");
    }

    private ResultActions enviar(String conteudo) throws Exception {
        return enviar(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions enviar(byte[] conteudo) throws Exception {
        return mvc.perform(multipart("/api/v1/importacoes")
                .file(new MockMultipartFile("arquivo", "acervo.csv", "text/csv", conteudo)));
    }

    private long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void processaCadaLinhaEContaOsResultados() throws Exception {
        String desconhecido = Dados.isbn(902);
        String planilha = """
                ISBN;Tombo;Localização
                %s;imp-2;Estante 3
                %s;;
                %s;IMP-3
                978-0-00-000000-0;IMP-4
                %s;IMP-5
                %s;IMP-5
                %s;IMP-1
                """.formatted(isbnNoAcervo, isbnNoAcervo, desconhecido, isbnNoAcervo, isbnNoAcervo, isbnNoAcervo);
        ResultActions r = enviar(planilha).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSANDO"))
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.pendentes").value(5))
                .andExpect(jsonPath("$.contagem.ERRO").value(2));
        long id = id(r);

        processador.processarTudo(id);
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/importacoes/" + id))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.finalizadaEm").isNotEmpty())
                .andExpect(jsonPath("$.pendentes").value(0))
                .andExpect(jsonPath("$.contagem.EXEMPLAR_ADICIONADO").value(2))
                .andExpect(jsonPath("$.contagem.JA_CADASTRADO").value(1))
                .andExpect(jsonPath("$.contagem.FONTE_INDISPONIVEL").value(1))
                .andExpect(jsonPath("$.contagem.ERRO").value(3));

        mvc.perform(get("/api/v1/importacoes/" + id + "/itens"))
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.itens[0].linha").value(2))
                .andExpect(jsonPath("$.itens[0].resultado").value("EXEMPLAR_ADICIONADO"))
                .andExpect(jsonPath("$.itens[0].tombo").value("IMP-2"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Quarto de Despejo"))
                .andExpect(jsonPath("$.itens[3].mensagem").value("O dígito verificador não confere. Confira o número."))
                .andExpect(jsonPath("$.itens[5].mensagem").value("Tombo IMP-5 repetido (já está na linha 6)."))
                .andExpect(jsonPath("$.itens[6].mensagem").value("Já existe um exemplar com o tombo IMP-1."));

        mvc.perform(get("/api/v1/importacoes/" + id + "/itens?resultado=FONTE_INDISPONIVEL"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].isbn13").value(desconhecido));

        Integer exemplares = jdbc.queryForObject("select count(*) from exemplar where obra_id = ?", Integer.class, obra);
        assertThat(exemplares).isEqualTo(3);
        assertThat(jdbc.queryForObject("select localizacao from exemplar where tombo = 'IMP-2'", String.class))
                .isEqualTo("Estante 3");
        assertThat(jdbc.queryForObject(
                "select count(*) from auditoria where acao = 'IMPORTACAO_INICIADA' and entidade_id = ?",
                Integer.class, id)).isEqualTo(1);
    }

    @Test
    void novaTentativaDasFontesForaDoAr() throws Exception {
        long id = id(enviar(Dados.isbn(903) + ";IMP-9\n").andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/importacoes/" + id + "/nova-tentativa")).andExpect(status().isUnprocessableEntity());

        processador.processarTudo(id);
        em.flush();
        em.clear();
        mvc.perform(post("/api/v1/importacoes/" + id + "/nova-tentativa")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSANDO"))
                .andExpect(jsonPath("$.pendentes").value(1));

        processador.processarTudo(id);
        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/importacoes/" + id))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.contagem.FONTE_INDISPONIVEL").value(1));
        mvc.perform(get("/api/v1/importacoes")).andExpect(jsonPath("$.itens[0].id").value(id));
    }

    @Test
    void planilhaDoExcelEmWindows1252() throws Exception {
        byte[] conteudo = (isbnNoAcervo + ";IMP-7;Estante Ação\r\n9,78857E+12;IMP-8\r\n")
                .getBytes(Charset.forName("windows-1252"));
        long id = id(enviar(conteudo).andExpect(status().isCreated()));
        processador.processarTudo(id);
        em.flush();
        em.clear();
        assertThat(jdbc.queryForObject("select localizacao from exemplar where tombo = 'IMP-7'", String.class))
                .isEqualTo("Estante Ação");
        mvc.perform(get("/api/v1/importacoes/" + id + "/itens?resultado=ERRO"))
                .andExpect(jsonPath("$.itens[0].mensagem").value(
                        "O ISBN virou número no Excel (9,78857E+12). Formate a coluna como texto e exporte de novo."));
    }

    @Test
    void planilhaVaziaOuSemPermissao() throws Exception {
        enviar("ISBN;Tombo\n\n").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A planilha não tem nenhuma linha com ISBN."));
        mvc.perform(get("/api/v1/importacoes/999999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoImporta() throws Exception {
        enviar(isbnNoAcervo + "\n").andExpect(status().isForbidden());
    }
}
