package br.gov.imbe.biblioteca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Chamadas ao site público nos testes: sem login e cada teste com o seu IP, porque o limite de tentativas por IP
 * vive no contexto compartilhado por todas as classes de teste.
 */
public final class Publico {

    private static final AtomicInteger IPS = new AtomicInteger();

    private Publico() {
    }

    /** IP novo a cada chamada (10.77.x.y). */
    public static RequestPostProcessor ipNovo() {
        int n = IPS.incrementAndGet();
        String ip = "10.77." + (n / 250) + "." + (n % 250 + 1);
        return deIp(ip);
    }

    public static RequestPostProcessor deIp(String ip) {
        return requisicao -> {
            requisicao.setRemoteAddr(ip);
            return anonymous().postProcessRequest(requisicao);
        };
    }

    public static MockHttpServletRequestBuilder postJson(String url, String json, RequestPostProcessor ip) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json).with(ip);
    }

    /** Reserva com um telefone fictício e sem e-mail. */
    public static ResultActions reservar(MockMvc mvc, RequestPostProcessor ip, long obraId, String cpf,
            LocalDate nascimento) throws Exception {
        return reservar(mvc, ip, obraId, cpf, nascimento, "(51) 90000-0000", null);
    }

    public static ResultActions reservar(MockMvc mvc, RequestPostProcessor ip, long obraId, String cpf,
            LocalDate nascimento, String telefone, String email) throws Exception {
        return mvc.perform(postJson("/api/v1/publico/reservas", """
                {"obraId": %d, "nome": "Leitora Fictícia da Silva", "cpf": "%s", "dataNascimento": "%s",
                 "telefone": %s, "email": %s}
                """.formatted(obraId, cpf, nascimento, json(telefone), json(email)), ip));
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    public static String identificacao(String cpf, LocalDate nascimento) {
        return """
                {"cpf": "%s", "dataNascimento": "%s"}
                """.formatted(cpf, nascimento);
    }
}
