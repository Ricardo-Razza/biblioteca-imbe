package br.gov.imbe.biblioteca.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.Publico;
import br.gov.imbe.biblioteca.TesteIntegracao;
import br.gov.imbe.biblioteca.comum.config.RelogioConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class RelatorioIntegracaoTest {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager em;

    final LocalDate hoje = LocalDate.now(RelogioConfig.FUSO);
    long leitor;
    long obra;

    @BeforeEach
    void preparar() throws Exception {
        leitor = Dados.leitor(mvc, "Rita Relatório");
        obra = Dados.obra(mvc, Dados.isbn(810), "Grande Sertão: Veredas");
        for (int i = 1; i <= 4; i++) {
            Dados.exemplar(mvc, obra, "REL-" + i);
        }
    }

    private long emprestar(String tombo) throws Exception {
        String corpo = mvc.perform(post("/api/v1/emprestimos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"leitorId\": " + leitor + ", \"tombo\": \"" + tombo + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private void prever(long emprestimoId, LocalDate saida, LocalDate prevista) {
        em.flush();
        em.clear();
        jdbc.update("update emprestimo set data_saida = ?, data_prevista = ? where id = ?", saida, prevista,
                emprestimoId);
    }

    private long contador(String json, String campo) {
        return ((Number) JsonPath.read(json, "$.contadores." + campo)).longValue();
    }

    /** O painel lê por SQL direto: o que o JPA ainda não gravou na transação do teste não aparece. */
    private String painel() throws Exception {
        em.flush();
        return mvc.perform(get("/api/v1/painel")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    private String csv(String url) throws Exception {
        em.flush();
        byte[] corpo = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.startsWith("text/csv")))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = new String(corpo, StandardCharsets.UTF_8);
        assertThat(texto).startsWith("﻿");
        return texto.substring(1);
    }

    @Test
    void painelContaOQuePrecisaDeAtencaoHoje() throws Exception {
        String antes = painel();
        long atrasado = emprestar("REL-1");
        long venceHoje = emprestar("REL-2");
        emprestar("REL-3");
        prever(atrasado, hoje.minusDays(20), hoje.minusDays(6));
        prever(venceHoje, hoje.minusDays(14), hoje);
        jdbc.update("update leitor set bloqueado_ate = ? where id = ?", hoje.plusDays(3), leitor);

        String depois = painel();
        assertThat(contador(depois, "emprestados") - contador(antes, "emprestados")).isEqualTo(3);
        assertThat(contador(depois, "atrasados") - contador(antes, "atrasados")).isEqualTo(1);
        assertThat(contador(depois, "venceHoje") - contador(antes, "venceHoje")).isEqualTo(1);
        assertThat(contador(depois, "leitoresBloqueados") - contador(antes, "leitoresBloqueados")).isEqualTo(1);

        mvc.perform(get("/api/v1/painel"))
                .andExpect(jsonPath("$.hoje").value(hoje.toString()))
                .andExpect(jsonPath("$.atrasados[?(@.id == " + atrasado + ")].diasAtraso").value(6))
                .andExpect(jsonPath("$.atrasados[?(@.id == " + atrasado + ")].tombo").value("REL-1"))
                .andExpect(jsonPath("$.venceHoje[?(@.id == " + venceHoje + ")].leitor").value("Rita Relatório"))
                .andExpect(jsonPath("$.movimento.length()").value(7))
                .andExpect(jsonPath("$.movimento[6].data").value(hoje.toString()))
                .andExpect(jsonPath("$.movimento[0].data").value(hoje.minusDays(6).toString()));
    }

    @Test
    void movimentoDaSemanaSomaSaidasEDevolucoes() throws Exception {
        String antes = painel();
        long id = emprestar("REL-1");
        mvc.perform(post("/api/v1/emprestimos/" + id + "/devolucao").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isOk());
        String depois = painel();
        long saidasAntes = ((Number) JsonPath.read(antes, "$.movimento[6].saidas")).longValue();
        long devolucoesAntes = ((Number) JsonPath.read(antes, "$.movimento[6].devolucoes")).longValue();
        assertThat(((Number) JsonPath.read(depois, "$.movimento[6].saidas")).longValue()).isEqualTo(saidasAntes + 1);
        assertThat(((Number) JsonPath.read(depois, "$.movimento[6].devolucoes")).longValue())
                .isEqualTo(devolucoesAntes + 1);
    }

    @Test
    void painelListaReservasParaSeparar() throws Exception {
        String cpf = Dados.cpf();
        LocalDate nascimento = LocalDate.of(1985, 3, 2);
        Dados.leitor(mvc, "Rui Reserva", cpf, nascimento);
        em.flush();
        em.clear();
        Publico.reservar(mvc, Publico.ipNovo(), obra, cpf, nascimento).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/painel"))
                .andExpect(jsonPath("$.paraSeparar[?(@.leitor == 'Rui Reserva')].titulo")
                        .value("Grande Sertão: Veredas"))
                .andExpect(jsonPath("$.contadores.reservasParaSeparar").value(Matchers.greaterThanOrEqualTo(1)));

        String texto = csv("/api/v1/relatorios/reservas.csv");
        assertThat(texto).startsWith("Protocolo;Feita em;Título;Tombo;Leitor;Situação");
        assertThat(texto).contains(";Grande Sertão: Veredas;REL-").contains(";Rui Reserva;Aguardando retirada;");
        assertThat(texto).containsPattern("\\r\\n[A-Z2-9]{4}-[A-Z2-9]{4};");
    }

    @Test
    void relatorioDeEmprestimosDoPeriodo() throws Exception {
        long antigo = emprestar("REL-1");
        emprestar("REL-2");
        prever(antigo, hoje.minusDays(60), hoje.minusDays(46));

        String texto = csv("/api/v1/relatorios/emprestimos.csv?de=" + hoje.minusDays(10) + "&ate=" + hoje);
        assertThat(texto).startsWith(
                "Saída;Devolução prevista;Devolvido em;Situação;Renovações;Ocorrência;Tombo;Título;Leitor\r\n");
        assertThat(texto).contains(BR.format(hoje) + ";").contains(";Em aberto;0;;REL-2;Grande Sertão: Veredas;Rita Relatório\r\n");
        assertThat(texto).doesNotContain(";REL-1;");

        String antigos = csv("/api/v1/relatorios/emprestimos.csv?de=" + hoje.minusDays(70) + "&ate="
                + hoje.minusDays(50));
        assertThat(antigos).contains(";Atrasado;0;;REL-1;");

        mvc.perform(get("/api/v1/relatorios/emprestimos.csv?de=2026-01-10&ate=2026-02-01"))
                .andExpect(header().string("Content-Disposition",
                        Matchers.containsString("emprestimos-2026-01-10-a-2026-02-01.csv")));
    }

    @Test
    void relatorioDeAtrasadosTrazContatoEDias() throws Exception {
        jdbc.update("update leitor set telefone = '51 99999-0000' where id = ?", leitor);
        long id = emprestar("REL-4");
        prever(id, hoje.minusDays(30), hoje.minusDays(16));

        String texto = csv("/api/v1/relatorios/atrasados.csv");
        assertThat(texto).startsWith("Tombo;Título;Leitor;Telefone;E-mail;Saída;Devolução prevista;Dias de atraso");
        assertThat(texto).contains("REL-4;Grande Sertão: Veredas;Rita Relatório;51 99999-0000;;"
                + BR.format(hoje.minusDays(30)) + ";" + BR.format(hoje.minusDays(16)) + ";16\r\n");
    }

    @Test
    void relatorioDoAcervoNeutralizaFormulas() throws Exception {
        long perigosa = Dados.obra(mvc, Dados.isbn(811), "=SOMA(1;2)");
        Dados.exemplar(mvc, perigosa, "REL-F");

        String texto = csv("/api/v1/relatorios/acervo.csv");
        assertThat(texto).startsWith("Tombo;ISBN;Título;Autores;Editora;Ano;Localização;Estado;Situação;Aquisição;Origem");
        assertThat(texto).contains("REL-F;" + Dados.isbn(811) + ";\"'=SOMA(1;2)\";");
        assertThat(texto).containsPattern("REL-1;\\d{13};Grande Sertão: Veredas;[^;]*;[^;]*;[^;]*;[^;]*;Bom;Disponível;");
    }

    @Test
    void maisEmprestadosOrdenaPorQuantidade() throws Exception {
        long outra = Dados.obra(mvc, Dados.isbn(812), "Obra Menos Lida");
        Dados.exemplar(mvc, outra, "REL-M");
        emprestar("REL-1");
        emprestar("REL-2");
        emprestar("REL-M");

        String texto = csv("/api/v1/relatorios/mais-emprestados.csv?de=" + hoje + "&ate=" + hoje);
        assertThat(texto).startsWith("Posição;Título;Autores;ISBN;Empréstimos\r\n");
        int grande = texto.indexOf("Grande Sertão: Veredas");
        int menos = texto.indexOf("Obra Menos Lida");
        assertThat(grande).isPositive().isLessThan(menos);
        assertThat(texto).containsPattern(";Grande Sertão: Veredas;[^;]*;\\d{13};2\\r\\n");
    }

    @Test
    void buscasSemResultadoSomamOsDiasDoPeriodo(@Autowired JdbcClient jdbc) throws Exception {
        // Termo inventado e datas de 2001: não cruza com o que outros testes gravam de verdade.
        String termo = "relatorio sem acervo " + Long.toString(System.nanoTime(), 36).replaceAll("\\d", "x");
        LocalDate dia = LocalDate.of(2001, 3, 10);
        jdbc.sql("insert into busca_sem_resultado (termo, dia, vezes) values (:t, :d, 2), (:t, :d2, 3)")
                .param("t", termo).param("d", dia).param("d2", dia.plusDays(1)).update();

        String texto = csv("/api/v1/relatorios/buscas-sem-resultado.csv?de=2001-03-01&ate=2001-03-31");
        assertThat(texto).startsWith("Termo;Buscas;Última busca\r\n").contains(termo + ";5;11/03/2001\r\n");
        assertThat(csv("/api/v1/relatorios/buscas-sem-resultado.csv?de=2001-04-01&ate=2001-04-30"))
                .doesNotContain(termo);
    }

    @Test
    void exportacaoFicaNaAuditoria() throws Exception {
        csv("/api/v1/relatorios/emprestimos.csv?de=2026-01-01&ate=2026-01-31");
        Integer n = jdbc.queryForObject("""
                select count(*) from auditoria
                 where acao = 'RELATORIO_EXPORTADO' and detalhe = 'Empréstimos de 01/01/2026 a 31/01/2026'
                """, Integer.class);
        assertThat(n).isEqualTo(1);
    }

    @Test
    void periodoInvalidoETipoDesconhecido() throws Exception {
        mvc.perform(get("/api/v1/relatorios/emprestimos.csv?de=2026-02-01&ate=2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial deve ser anterior ou igual à final."));
        mvc.perform(get("/api/v1/relatorios/emprestimos.csv?de=2010-01-01&ate=2026-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/relatorios/senhas.csv")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteVeOPainelEExporta() throws Exception {
        mvc.perform(get("/api/v1/painel")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/relatorios/acervo.csv")).andExpect(status().isOk());
    }
}
