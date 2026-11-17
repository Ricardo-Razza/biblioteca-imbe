package br.gov.imbe.biblioteca.comum.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.TesteIntegracao;
import br.gov.imbe.biblioteca.comum.calendario.FeriadosBrasilApi.FeriadoExterno;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class FeriadoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    FeriadoService servico;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void gravaNacionaisSemSobrescreverMunicipais() {
        jdbc.update("insert into feriado (data, descricao, tipo) values ('2031-04-21', 'Feriado municipal', 'MUNICIPAL')");
        jdbc.update("insert into feriado (data, descricao, tipo) values ('2031-01-01', 'Ano novo', 'NACIONAL')");
        FeriadoService.Importacao r = servico.gravarNacionais(2031, List.of(
                new FeriadoExterno(LocalDate.of(2031, 1, 1), "Confraternização mundial", "national"),
                new FeriadoExterno(LocalDate.of(2031, 4, 21), "Tiradentes", "national"),
                new FeriadoExterno(LocalDate.of(2031, 9, 7), "Independência do Brasil", "national"),
                new FeriadoExterno(LocalDate.of(2032, 1, 1), "Outro ano", "national")));

        assertThat(r).isEqualTo(new FeriadoService.Importacao(2031, 1, 1));
        assertThat(servico.doAno(2031)).extracting(FeriadoService.FeriadoResposta::descricao)
                .containsExactly("Confraternização mundial", "Feriado municipal", "Independência do Brasil");
        assertThat(servico.temNacionais(2031)).isTrue();
        assertThat(servico.temNacionais(2032)).isFalse();
    }

    @Test
    void importacaoComBrasilApiForaDoArResponde503() throws Exception {
        mvc.perform(post("/api/v1/feriados/importacao").param("ano", "2031"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(
                        "Não foi possível buscar os feriados nacionais agora. Tente mais tarde."));
        mvc.perform(get("/api/v1/feriados").param("ano", "2031")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoImporta() throws Exception {
        mvc.perform(post("/api/v1/feriados/importacao")).andExpect(status().isForbidden());
    }
}
