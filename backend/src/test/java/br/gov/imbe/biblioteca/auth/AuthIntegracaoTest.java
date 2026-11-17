package br.gov.imbe.biblioteca.auth;

import static br.gov.imbe.biblioteca.Funcionarios.SENHA;
import static br.gov.imbe.biblioteca.Funcionarios.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.Funcionarios;
import br.gov.imbe.biblioteca.TesteIntegracao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
class AuthIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder senhas;

    @PersistenceContext
    EntityManager em;

    private ResultActions login(String login, String senha) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"%s\", \"senha\": \"%s\"}".formatted(login, senha)));
    }

    private long auditorias(String acao, long funcionarioId) {
        return jdbc.queryForObject("select count(*) from auditoria where acao = ? and funcionario_id = ?",
                Long.class, acao, funcionarioId);
    }

    @Test
    void semTokenRecusaComProblemDetailEmPortugues() throws Exception {
        mvc.perform(get("/api/v1/leitores"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Entre no sistema para continuar."));
        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginLocalDevolveTokenQueAbreAApi() throws Exception {
        long id = Funcionarios.local(jdbc, senhas, "maria.atendente", "ATENDENTE");

        login("  Maria.Atendente ", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.funcionario.id").value(id))
                .andExpect(jsonPath("$.funcionario.perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.funcionario.trocarSenha").value(false));

        String token = Funcionarios.token(mvc, "maria.atendente", SENHA);
        mvc.perform(get("/api/v1/auth/eu").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("maria.atendente"));
        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        assertThat(auditorias("LOGIN", id)).isEqualTo(2);
    }

    @Test
    void senhaErradaELoginInexistenteTemAMesmaMensagem() throws Exception {
        Funcionarios.local(jdbc, senhas, "joao", "ATENDENTE");
        login("joao", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login ou senha incorretos."));
        login("ninguem", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login ou senha incorretos."));
        assertThat(jdbc.queryForObject("select count(*) from auditoria where acao = 'LOGIN_RECUSADO'"
                + " and detalhe like '%ninguem%'", Long.class)).isEqualTo(1);
    }

    @Test
    void cincoSenhasErradasTravamOLogin() throws Exception {
        long id = Funcionarios.local(jdbc, senhas, "travado", "ATENDENTE");
        for (int i = 0; i < 4; i++) {
            login("travado", "errada").andExpect(jsonPath("$.detail").value("Login ou senha incorretos."));
        }
        login("travado", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Muitas tentativas erradas")));
        // Nem a senha certa entra enquanto estiver travado
        login("travado", SENHA).andExpect(status().isUnauthorized());
        assertThat(auditorias("LOGIN_TRAVADO", id)).isEqualTo(1);
    }

    @Test
    void acertarZeraAsTentativas() throws Exception {
        Funcionarios.local(jdbc, senhas, "distraido", "ATENDENTE");
        for (int i = 0; i < 4; i++) {
            login("distraido", "errada");
        }
        login("distraido", SENHA).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login("distraido", "errada");
        }
        login("distraido", SENHA).andExpect(status().isOk());
    }

    @Test
    void senhaProvisoriaSoDeixaTrocarASenha() throws Exception {
        Funcionarios.inserir(jdbc, "novato", "LOCAL", senhas.encode("Provisoria9"), "ATENDENTE", true);
        login("novato", "Provisoria9").andExpect(jsonPath("$.funcionario.trocarSenha").value(true));
        String token = Funcionarios.token(mvc, "novato", "Provisoria9");

        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Troque a senha provisória antes de continuar."));

        trocar(token, "Provisoria9", "curta1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha precisa ter pelo menos 8 caracteres."));
        trocar(token, "Provisoria9", "somenteletras").andExpect(status().isBadRequest());
        trocar(token, "errada", "NovaSenha2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha atual não confere."));
        trocar(token, "Provisoria9", "NovaSenha2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.trocarSenha").value(false));

        // O mesmo token já vale: a troca de senha é lida do banco a cada requisição
        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        login("novato", "Provisoria9").andExpect(status().isUnauthorized());
        login("novato", "NovaSenha2026").andExpect(status().isOk());
    }

    private ResultActions trocar(String token, String atual, String nova) throws Exception {
        return mvc.perform(post("/api/v1/auth/senha").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"%s\", \"novaSenha\": \"%s\"}".formatted(atual, nova)));
    }

    @Test
    void funcionarioDesativadoPerdeOAcessoNaHora() throws Exception {
        long id = Funcionarios.local(jdbc, senhas, "saiu", "ATENDENTE");
        String token = Funcionarios.token(mvc, "saiu", SENHA);
        em.flush();
        em.clear();
        jdbc.update("update funcionario set ativo = false where id = ?", id);

        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        login("saiu", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void atendenteNaoMexeNaAdministracao() throws Exception {
        Funcionarios.local(jdbc, senhas, "balcao", "ATENDENTE");
        String token = Funcionarios.token(mvc, "balcao", SENHA);

        mvc.perform(put("/api/v1/parametros/PRAZO_EMPRESTIMO_DIAS").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Seu perfil não permite esta operação."));
        mvc.perform(get("/api/v1/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/parametros").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void adminTambemFazOQueOAtendenteFaz() throws Exception {
        Funcionarios.local(jdbc, senhas, "chefe", "ADMIN");
        String token = Funcionarios.token(mvc, "chefe", SENHA);
        mvc.perform(get("/api/v1/leitores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void emprestimoGuardaQuemRegistrou() throws Exception {
        long funcionario = Funcionarios.local(jdbc, senhas, "registra", "ADMIN");
        String token = Funcionarios.token(mvc, "registra", SENHA);
        String auth = bearer(token);

        long obra = idCom(auth, "/api/v1/obras", """
                {"isbn": "%s", "titulo": "Livro do balcão", "autores": ["Autor"]}
                """.formatted(Dados.isbn(777_001)));
        idCom(auth, "/api/v1/obras/" + obra + "/exemplares", "{\"tombo\": \"AUTH-1\"}");
        long leitor = idCom(auth, "/api/v1/leitores", """
                {"nome": "Leitor do balcão", "cpf": "%s", "dataNascimento": "1990-01-01"}
                """.formatted(Dados.cpf()));
        long emprestimo = idCom(auth, "/api/v1/emprestimos",
                "{\"leitorId\": %d, \"tombo\": \"AUTH-1\"}".formatted(leitor));

        mvc.perform(post("/api/v1/emprestimos/" + emprestimo + "/devolucao").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
        em.flush();
        assertThat(jdbc.queryForMap("select funcionario_saida_id s, funcionario_devolucao_id d from emprestimo"
                + " where id = ?", emprestimo))
                .containsEntry("s", funcionario).containsEntry("d", funcionario);
    }

    private long idCom(String auth, String url, String json) throws Exception {
        String corpo = mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(corpo, "$.id")).longValue();
    }

    @Test
    void capasELoginSaoPublicos() throws Exception {
        mvc.perform(get("/api/v1/obras/999999/capa")).andExpect(status().isNotFound());
        login("", "").andExpect(status().isBadRequest());
    }
}
