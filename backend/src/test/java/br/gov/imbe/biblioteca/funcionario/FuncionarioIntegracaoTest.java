package br.gov.imbe.biblioteca.funcionario;

import static br.gov.imbe.biblioteca.Funcionarios.SENHA;
import static br.gov.imbe.biblioteca.Funcionarios.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.Funcionarios;
import br.gov.imbe.biblioteca.TesteIntegracao;

/** Cadastro de funcionários, auditoria e feriados, com o token de um administrador de verdade. */
@TesteIntegracao
@Transactional
class FuncionarioIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder senhas;

    long adminId;
    String auth;

    @BeforeEach
    void entrarComoAdmin() throws Exception {
        adminId = Funcionarios.local(jdbc, senhas, "admin.teste", "ADMIN");
        auth = bearer(Funcionarios.token(mvc, "admin.teste", SENHA));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder req, String json) throws Exception {
        return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void cadastraLocalComSenhaProvisoriaQueSoServeParaTrocar() throws Exception {
        String corpo = enviar(post("/api/v1/funcionarios"), """
                {"nome": " Ana Souza ", "login": "Ana.Souza", "tipo": "LOCAL", "perfil": "ATENDENTE"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.funcionario.login").value("ana.souza"))
                .andExpect(jsonPath("$.funcionario.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.funcionario.trocarSenha").value(true))
                .andExpect(jsonPath("$.senhaProvisoria").value(Matchers.matchesPattern("[A-Za-z2-9]{10}")))
                .andReturn().getResponse().getContentAsString();
        String provisoria = JsonPath.read(corpo, "$.senhaProvisoria");

        String token = Funcionarios.token(mvc, "ana.souza", provisoria);
        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        enviar(post("/api/v1/funcionarios"), """
                {"nome": "Outra Ana", "login": "ana.souza", "tipo": "AD", "perfil": "ATENDENTE"}
                """).andExpect(status().isConflict());
    }

    @Test
    void usuarioAdNaoTemSenhaNoBanco() throws Exception {
        String corpo = enviar(post("/api/v1/funcionarios"), """
                {"nome": "Carlos Rede", "login": "carlos", "tipo": "AD", "perfil": "ATENDENTE"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senhaProvisoria").doesNotExist())
                .andExpect(jsonPath("$.funcionario.trocarSenha").value(false))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(corpo, "$.funcionario.id");
        mvc.perform(post("/api/v1/funcionarios/" + id + "/senha-provisoria").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void validaOCadastro() throws Exception {
        enviar(post("/api/v1/funcionarios"), """
                {"nome": "", "login": "a b", "tipo": null, "perfil": "ATENDENTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", Matchers.containsInAnyOrder("login", "nome", "tipo")));
    }

    @Test
    void naoDesativaASiMesmoNemDeixaSemAdmin() throws Exception {
        enviar(put("/api/v1/funcionarios/" + adminId), """
                {"nome": "Admin", "perfil": "ADMIN", "ativo": false}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Você não pode desativar o seu próprio usuário."));
        enviar(put("/api/v1/funcionarios/" + adminId), """
                {"nome": "Admin", "perfil": "ATENDENTE", "ativo": true}
                """).andExpect(status().isUnprocessableContent());

        long outro = Funcionarios.local(jdbc, senhas, "outro.admin", "ADMIN");
        enviar(put("/api/v1/funcionarios/" + outro), """
                {"nome": "Outro Admin", "perfil": "ATENDENTE", "ativo": true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"));
    }

    @Test
    void redefinirSenhaDaNovaProvisoriaEAuditoriaRegistra() throws Exception {
        long id = Funcionarios.local(jdbc, senhas, "esqueceu", "ATENDENTE");
        String corpo = mvc.perform(post("/api/v1/funcionarios/" + id + "/senha-provisoria")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionario.trocarSenha").value(true))
                .andReturn().getResponse().getContentAsString();
        String provisoria = JsonPath.read(corpo, "$.senhaProvisoria");
        Funcionarios.token(mvc, "esqueceu", provisoria);

        mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].acao").value("LOGIN"))
                .andExpect(jsonPath("$.itens[1].acao").value("SENHA_REDEFINIDA"))
                .andExpect(jsonPath("$.itens[1].funcionario").value("Funcionário admin.teste"))
                .andExpect(jsonPath("$.itens[1].descricao").value("Redefiniu a senha de um funcionário"));
        mvc.perform(get("/api/v1/auditoria?acao=SENHA_REDEFINIDA").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void alterarParametroFicaNaAuditoria() throws Exception {
        enviar(put("/api/v1/parametros/PRAZO_EMPRESTIMO_DIAS"), "{\"valor\": 21}").andExpect(status().isOk());
        mvc.perform(get("/api/v1/auditoria?acao=PARAMETRO_ALTERADO").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.itens[0].detalhe").value("PRAZO_EMPRESTIMO_DIAS: 14 → 21"));
    }

    @Test
    void feriadosMunicipais() throws Exception {
        enviar(post("/api/v1/feriados"), """
                {"data": "2031-03-15", "descricao": "Aniversário de Imbé"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("MUNICIPAL"));
        enviar(post("/api/v1/feriados"), """
                {"data": "2031-03-15", "descricao": "Repetido"}
                """).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/feriados?ano=2031").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$[*].descricao", Matchers.hasItem("Aniversário de Imbé")));
        mvc.perform(delete("/api/v1/feriados/2031-03-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/feriados/2031-03-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
