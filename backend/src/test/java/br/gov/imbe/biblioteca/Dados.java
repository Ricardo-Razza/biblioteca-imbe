package br.gov.imbe.biblioteca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Cria obras, exemplares e leitores pela API nos testes de integração. */
public final class Dados {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(100_000_000);

    private Dados() {
    }

    /** CPF válido e diferente a cada chamada. */
    public static String cpf() {
        String base = String.valueOf(SEQUENCIA.incrementAndGet());
        int d1 = digito(base, 10);
        int d2 = digito(base + d1, 11);
        return base + d1 + d2;
    }

    private static int digito(String numeros, int peso) {
        int soma = 0;
        for (char c : numeros.toCharArray()) {
            soma += (c - '0') * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** ISBN-13 válido a partir de 9 dígitos variáveis: 978 + 9 + dígito. */
    public static String isbn(int n) {
        String doze = "978" + String.format("%09d", n);
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (doze.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return doze + ((10 - soma % 10) % 10);
    }

    public static long obra(MockMvc mvc, String isbn, String titulo) throws Exception {
        return id(mvc, "/api/v1/obras", """
                {"isbn": "%s", "titulo": "%s", "autores": ["Autor de Teste"]}
                """.formatted(isbn, titulo));
    }

    public static long exemplar(MockMvc mvc, long obraId, String tombo) throws Exception {
        return id(mvc, "/api/v1/obras/" + obraId + "/exemplares", "{\"tombo\": \"" + tombo + "\"}");
    }

    public static long leitor(MockMvc mvc, String nome) throws Exception {
        return id(mvc, "/api/v1/leitores", """
                {"nome": "%s", "cpf": "%s", "dataNascimento": "1990-05-10"}
                """.formatted(nome, cpf()));
    }

    public static long leitorMenor(MockMvc mvc, String nome) throws Exception {
        return id(mvc, "/api/v1/leitores", """
                {"nome": "%s", "cpf": "%s", "dataNascimento": "%s", "responsavel": "Responsável de Teste"}
                """.formatted(nome, cpf(), LocalDate.now().minusYears(10)));
    }

    public static long id(MockMvc mvc, String url, String json) throws Exception {
        String corpo = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }
}
