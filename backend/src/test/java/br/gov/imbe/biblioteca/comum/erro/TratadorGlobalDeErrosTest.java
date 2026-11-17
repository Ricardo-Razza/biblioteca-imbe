package br.gov.imbe.biblioteca.comum.erro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.auth.ConversorJwt;
import br.gov.imbe.biblioteca.comum.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@WebMvcTest(controllers = TratadorGlobalDeErrosTest.ControllerDeTeste.class)
@Import({ SecurityConfig.class, TratadorGlobalDeErros.class, TratadorGlobalDeErrosTest.ControllerDeTeste.class })
@WithMockUser(roles = "ATENDENTE")
class TratadorGlobalDeErrosTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    ConversorJwt conversorJwt;

    record Pedido(@NotBlank(message = "Informe o título.") String titulo,
            @Positive(message = "Deve ser maior que zero.") Integer paginas) {
    }

    @RestController
    static class ControllerDeTeste {

        @GetMapping("/api/v1/teste/nao-encontrado")
        void naoEncontrado() {
            throw new RecursoNaoEncontradoException("Obra 42 não encontrada.");
        }

        @GetMapping("/api/v1/teste/regra")
        void regra() {
            throw new RegraNegocioException("Leitor com empréstimo em atraso.");
        }

        @GetMapping("/api/v1/teste/conflito")
        void conflito() {
            throw new ConflitoException("ISBN já cadastrado.");
        }

        @GetMapping("/api/v1/teste/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe interno que não pode vazar");
        }

        @GetMapping("/api/v1/teste/numero/{id}")
        Long numero(@PathVariable Long id) {
            return id;
        }

        @PostMapping("/api/v1/teste/validacao")
        void validacao(@Valid @RequestBody Pedido pedido) {
        }
    }

    @Test
    void recursoNaoEncontradoVira404() throws Exception {
        mvc.perform(get("/api/v1/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Obra 42 não encontrada."));
    }

    @Test
    void regraDeNegocioVira422() throws Exception {
        mvc.perform(get("/api/v1/teste/regra"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Operação não permitida"))
                .andExpect(jsonPath("$.detail").value("Leitor com empréstimo em atraso."));
    }

    @Test
    void conflitoVira409() throws Exception {
        mvc.perform(get("/api/v1/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("ISBN já cadastrado."));
    }

    @Test
    void erroInesperadoNaoVazaDetalhe() throws Exception {
        mvc.perform(get("/api/v1/teste/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value(
                        "Ocorreu um erro inesperado. Tente novamente; se continuar, avise a equipe de TI."));
    }

    @Test
    void validacaoListaOsCampos() throws Exception {
        mvc.perform(post("/api/v1/teste/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\": \"\", \"paginas\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos.length()").value(2))
                .andExpect(jsonPath("$.campos[0].campo").value("paginas"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Deve ser maior que zero."))
                .andExpect(jsonPath("$.campos[1].campo").value("titulo"))
                .andExpect(jsonPath("$.campos[1].mensagem").value("Informe o título."));
    }

    @Test
    void jsonMalFormadoTemMensagemEmPortugues() throws Exception {
        mvc.perform(post("/api/v1/teste/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("O conteúdo enviado não pôde ser lido. Confira o JSON."));
    }

    @Test
    void parametroComTipoErradoTemMensagemEmPortugues() throws Exception {
        mvc.perform(get("/api/v1/teste/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("O valor informado em id é inválido."));
    }

    @Test
    void rotaInexistenteVira404EmPortugues() throws Exception {
        mvc.perform(get("/api/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("O endereço pedido não existe."));
    }

    @Test
    void metodoNaoPermitidoVira405EmPortugues() throws Exception {
        mvc.perform(delete("/api/v1/teste/regra"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método não permitido"))
                .andExpect(jsonPath("$.detail").value("Este endereço não aceita o método DELETE."));
    }
}
