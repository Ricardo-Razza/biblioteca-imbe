package br.gov.imbe.biblioteca.publico;

import static br.gov.imbe.biblioteca.Publico.identificacao;
import static br.gov.imbe.biblioteca.Publico.ipNovo;
import static br.gov.imbe.biblioteca.Publico.postJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.Publico;
import br.gov.imbe.biblioteca.TesteIntegracao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class PublicoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @PersistenceContext
    EntityManager em;

    RequestPostProcessor ip;
    long disponivel;
    long emprestada;
    long semExemplar;

    @BeforeEach
    void preparar() throws Exception {
        ip = ipNovo();
        disponivel = Dados.id(mvc, "/api/v1/obras", """
                {"isbn": "%s", "titulo": "Quincas Borba Público", "autores": ["Machado de Assis"],
                 "assuntos": ["Romance brasileiro"], "sinopse": "Rubião herda a fortuna."}
                """.formatted(Dados.isbn(900)));
        Dados.exemplar(mvc, disponivel, "P-1");
        Dados.exemplar(mvc, disponivel, "P-2");

        emprestada = Dados.id(mvc, "/api/v1/obras", """
                {"isbn": "%s", "titulo": "Dom Casmurro Público", "autores": ["Machado de Assis"],
                 "assuntos": ["Romance brasileiro", "Clássico"]}
                """.formatted(Dados.isbn(901)));
        Dados.exemplar(mvc, emprestada, "P-10");
        long leitor = Dados.leitor(mvc, "Leitor do Catálogo");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/emprestimos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leitorId\": " + leitor + ", \"tombo\": \"P-10\"}")).andExpect(status().isCreated());

        semExemplar = Dados.obra(mvc, Dados.isbn(902), "Esaú e Jacó Público");
        // Os contadores de exemplares da obra são @Formula: só se atualizam ao recarregar
        em.flush();
        em.clear();
    }

    @Test
    void catalogoMostraSoObrasEmCirculacaoESemDadosInternos() throws Exception {
        String corpo = mvc.perform(get("/api/v1/publico/obras").param("q", "público").with(ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[0].titulo").value("Dom Casmurro Público"))
                .andExpect(jsonPath("$.itens[0].disponiveis").value(0))
                .andExpect(jsonPath("$.itens[1].titulo").value("Quincas Borba Público"))
                .andExpect(jsonPath("$.itens[1].disponiveis").value(2))
                .andExpect(jsonPath("$.itens[1].autores[0]").value("Machado de Assis"))
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContain("P-1").doesNotContain("Leitor do Catálogo").doesNotContain("isbn13");
    }

    @Test
    void buscaPorAutorAssuntoEFiltros() throws Exception {
        mvc.perform(get("/api/v1/publico/obras").param("q", "machado de assis").with(ip))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/v1/publico/obras").param("q", "CLASSICO").with(ip))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(emprestada));
        mvc.perform(get("/api/v1/publico/obras").param("q", "machado").param("disponiveis", "true").with(ip))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(disponivel));

        String assuntos = mvc.perform(get("/api/v1/publico/assuntos").with(ip))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer classico = JsonPath.<java.util.List<Integer>>read(assuntos, "$[?(@.nome == 'Clássico')].id").getFirst();
        assertThat(JsonPath.<java.util.List<Integer>>read(assuntos, "$[?(@.nome == 'Romance brasileiro')].obras"))
                .containsExactly(2);
        mvc.perform(get("/api/v1/publico/obras").param("assunto", classico.toString()).with(ip))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void fichaDoLivro() throws Exception {
        mvc.perform(get("/api/v1/publico/obras/" + disponivel).with(ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinopse").value("Rubião herda a fortuna."))
                .andExpect(jsonPath("$.assuntos[0].nome").value("Romance brasileiro"))
                .andExpect(jsonPath("$.disponiveis").value(2));
        mvc.perform(get("/api/v1/publico/obras/" + semExemplar).with(ip))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Livro não encontrado no catálogo."));
    }

    @Test
    void obraSoComExemplarBaixadoSaiDoCatalogo() throws Exception {
        long novo = Dados.exemplar(mvc, semExemplar, "P-20");
        mvc.perform(get("/api/v1/publico/obras/" + semExemplar).with(ip)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/exemplares/" + novo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\": \"BAIXADO\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/publico/obras/" + semExemplar).with(ip)).andExpect(status().isNotFound());
    }

    @Test
    void dadosDaBibliotecaParaORodape() throws Exception {
        mvc.perform(get("/api/v1/publico/biblioteca").with(ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Biblioteca Municipal de Imbé"));
    }

    @Test
    void limitaIdentificacoesPorIp() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(postJson("/api/v1/publico/reservas/consulta",
                    identificacao(Dados.cpf(), LocalDate.of(1990, 1, 1)), ip))
                    .andExpect(status().isUnprocessableContent());
        }
        mvc.perform(postJson("/api/v1/publico/reservas/consulta", identificacao(Dados.cpf(), LocalDate.of(1990, 1, 1)),
                ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Muitas tentativas")));
        mvc.perform(get("/api/v1/publico/obras").with(ip)).andExpect(status().isOk());
    }

    @Test
    void limitaIdentificacoesPorCpfVindasDeVariosIps() throws Exception {
        String cpf = Dados.cpf();
        for (int i = 0; i < 10; i++) {
            mvc.perform(postJson("/api/v1/publico/reservas/consulta", identificacao(cpf, LocalDate.of(1990, 1, 1 + i)),
                    Publico.ipNovo())).andExpect(status().isUnprocessableContent());
        }
        mvc.perform(postJson("/api/v1/publico/reservas/consulta", identificacao(cpf, LocalDate.of(1990, 2, 1)),
                Publico.ipNovo())).andExpect(status().isTooManyRequests());
    }
}
