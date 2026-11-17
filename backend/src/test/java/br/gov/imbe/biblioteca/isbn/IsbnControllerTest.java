package br.gov.imbe.biblioteca.isbn;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.gov.imbe.biblioteca.acervo.CapaService;
import br.gov.imbe.biblioteca.acervo.ObraService;
import br.gov.imbe.biblioteca.comum.config.SecurityConfig;

@WebMvcTest(IsbnController.class)
@Import(SecurityConfig.class)
class IsbnControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IsbnLookupService servico;

    @MockitoBean
    ObraService obras;

    @MockitoBean
    CapaService capas;

    @Test
    void obraJaCadastradaNaoConsultaAsFontes() throws Exception {
        given(obras.idPorIsbn(Isbn.de("9788545702870"))).willReturn(Optional.of(42L));

        mvc.perform(get("/api/v1/isbn/9788545702870"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obraId").value(42))
                .andExpect(jsonPath("$.encontrado").value(false))
                .andExpect(jsonPath("$.dados").doesNotExist());

        verifyNoInteractions(servico);
    }

    @Test
    void devolveOsDadosSemAUrlExternaDaCapa() throws Exception {
        Isbn isbn = Isbn.de("9788545702870");
        DadosLivro dados = new DadosLivro(isbn.valor(), "Akira", null, List.of("Katsuhiro Otomo"), "JBC", 2017,
                364, "pt", "Tóquio, 2019.", List.of("Mangá"), "https://externo/capa.jpg", FonteIsbn.BRASILAPI);
        given(servico.buscar(isbn)).willReturn(new ResultadoIsbn(isbn, Optional.of(dados),
                List.of(FonteIsbn.BRASILAPI, FonteIsbn.GOOGLE_BOOKS), List.of(FonteIsbn.OPEN_LIBRARY)));

        mvc.perform(get("/api/v1/isbn/978-85-457-0287-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn13").value("9788545702870"))
                .andExpect(jsonPath("$.obraId").doesNotExist())
                .andExpect(jsonPath("$.encontrado").value(true))
                .andExpect(jsonPath("$.dados.titulo").value("Akira"))
                .andExpect(jsonPath("$.dados.autores[0]").value("Katsuhiro Otomo"))
                .andExpect(jsonPath("$.dados.capaDisponivel").value(true))
                .andExpect(jsonPath("$.dados.capaUrl").doesNotExist())
                .andExpect(jsonPath("$.dados.fonte").value("BRASILAPI"))
                .andExpect(jsonPath("$.fontes.length()").value(2))
                .andExpect(jsonPath("$.fontesIndisponiveis[0]").value("OPEN_LIBRARY"));
    }

    @Test
    void naoEncontradoDevolve200ComEncontradoFalso() throws Exception {
        Isbn isbn = Isbn.de("0306406152");
        given(servico.buscar(isbn)).willReturn(new ResultadoIsbn(isbn, Optional.empty(), List.of(), List.of()));

        mvc.perform(get("/api/v1/isbn/0306406152"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn13").value("9780306406157"))
                .andExpect(jsonPath("$.encontrado").value(false))
                .andExpect(jsonPath("$.dados").doesNotExist());
    }

    @Test
    void isbnInvalidoDevolve400SemConsultarAsFontes() throws Exception {
        mvc.perform(get("/api/v1/isbn/9788545702871"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ISBN inválido"))
                .andExpect(jsonPath("$.detail").value("O dígito verificador não confere. Confira o número."));

        verifyNoInteractions(servico);
    }
}
