package br.gov.imbe.biblioteca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Funcionários e tokens nos testes de integração. */
public final class Funcionarios {

    public static final String SENHA = "senha-de-teste-1";

    private Funcionarios() {
    }

    /** Funcionário LOCAL ativo com {@link #SENHA}, sem troca pendente. */
    public static long local(JdbcTemplate jdbc, PasswordEncoder senhas, String login, String perfil) {
        return inserir(jdbc, login, "LOCAL", senhas.encode(SENHA), perfil, false);
    }

    public static long inserir(JdbcTemplate jdbc, String login, String tipo, String hash, String perfil,
            boolean trocarSenha) {
        GeneratedKeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into funcionario (nome, login, tipo_autenticacao, senha_hash, perfil, ativo, trocar_senha,
                        tentativas_falhas, criado_em)
                    values (?, ?, ?, ?, ?, true, ?, 0, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "Funcionário " + login);
            ps.setString(2, login);
            ps.setString(3, tipo);
            ps.setString(4, hash);
            ps.setString(5, perfil);
            ps.setBoolean(6, trocarSenha);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, chave);
        return Objects.requireNonNull(chave.getKey()).longValue();
    }

    public static String token(MockMvc mvc, String login, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"%s\", \"senha\": \"%s\"}".formatted(login, senha)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
