package br.gov.imbe.biblioteca.leitor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.TesteIntegracao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class LeitorIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager em;

    private ResultActions cadastrar(String json) throws Exception {
        return mvc.perform(post("/api/v1/leitores").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadastraNormalizandoCpfEEmail() throws Exception {
        String cpf = Dados.cpf();
        String formatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-"
                + cpf.substring(9);
        cadastrar("""
                {"nome": " Maria da Silva ", "cpf": "%s", "dataNascimento": "1985-03-02",
                 "email": "Maria@Exemplo.com", "telefone": "51 99999-0000"}
                """.formatted(formatado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.menorDeIdade").value(false));
    }

    @Test
    void cpfInvalidoERepetido() throws Exception {
        cadastrar("{\"nome\": \"A\", \"cpf\": \"123.456.789-00\", \"dataNascimento\": \"1990-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cpf"));
        String cpf = Dados.cpf();
        String json = "{\"nome\": \"A\", \"cpf\": \"" + cpf + "\", \"dataNascimento\": \"1990-01-01\"}";
        cadastrar(json).andExpect(status().isCreated());
        cadastrar(json)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um leitor com este CPF."));
    }

    @Test
    void menorDeIdadeExigeResponsavel() throws Exception {
        cadastrar("""
                {"nome": "Joãozinho", "cpf": "%s", "dataNascimento": "%s"}
                """.formatted(Dados.cpf(), LocalDate.now().minusYears(12)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("responsavel"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Leitor menor de idade: informe o responsável."));
    }

    @Test
    void buscaPorNomeSemAcentoEPeloComecoDoCpf() throws Exception {
        String cpf = Dados.cpf();
        cadastrar("{\"nome\": \"José Araújo\", \"cpf\": \"" + cpf + "\", \"dataNascimento\": \"1970-07-07\"}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/leitores").param("q", "jose araujo"))
                .andExpect(jsonPath("$.itens[*].nome", Matchers.hasItem("José Araújo")));
        mvc.perform(get("/api/v1/leitores").param("q", cpf.substring(0, 3) + "." + cpf.substring(3, 6)))
                .andExpect(jsonPath("$.itens[*].cpf", Matchers.hasItem(cpf)));
    }

    @Test
    void naoDesativaQuemEstaComLivro() throws Exception {
        long leitor = Dados.leitor(mvc, "Ana Leitora");
        long obra = Dados.obra(mvc, Dados.isbn(501), "Livro A");
        Dados.exemplar(mvc, obra, "L-501");
        mvc.perform(post("/api/v1/emprestimos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"leitorId\": " + leitor + ", \"tombo\": \"L-501\"}")).andExpect(status().isCreated());

        String json = """
                {"nome": "Ana Leitora", "cpf": "%s", "dataNascimento": "1990-05-10", "ativo": false}
                """.formatted(jdbc.queryForObject("select cpf from leitor where id = ?", String.class, leitor));
        mvc.perform(put("/api/v1/leitores/" + leitor).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void desbloqueio() throws Exception {
        long leitor = Dados.leitor(mvc, "Bia Bloqueada");
        em.flush();
        em.clear();
        jdbc.update("update leitor set bloqueado_ate = ? where id = ?", LocalDate.now().plusDays(5), leitor);
        mvc.perform(get("/api/v1/leitores/" + leitor)).andExpect(jsonPath("$.bloqueadoAte").exists());
        mvc.perform(post("/api/v1/leitores/" + leitor + "/desbloqueio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueadoAte").doesNotExist());
    }
}
