package br.gov.imbe.biblioteca.reserva;

import static br.gov.imbe.biblioteca.Publico.identificacao;
import static br.gov.imbe.biblioteca.Publico.ipNovo;
import static br.gov.imbe.biblioteca.Publico.postJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.gov.imbe.biblioteca.Dados;
import br.gov.imbe.biblioteca.Publico;
import br.gov.imbe.biblioteca.TesteIntegracao;
import br.gov.imbe.biblioteca.comum.calendario.Calendario;
import br.gov.imbe.biblioteca.comum.config.RelogioConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TesteIntegracao
@Transactional
@WithMockUser(roles = "ADMIN")
class ReservaIntegracaoTest {

    static final LocalDate NASCIMENTO = LocalDate.of(1988, 3, 15);

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Calendario calendario;

    @Autowired
    ReservaService servico;

    @PersistenceContext
    EntityManager em;

    final LocalDate hoje = LocalDate.now(RelogioConfig.FUSO);
    RequestPostProcessor ip;
    String cpf;
    long leitor;
    long obra;

    @BeforeEach
    void preparar() throws Exception {
        ip = ipNovo();
        cpf = Dados.cpf();
        leitor = Dados.leitor(mvc, "Joana Prado Lima", cpf, NASCIMENTO);
        obra = Dados.obra(mvc, Dados.isbn(800), "Vidas Secas");
        Dados.exemplar(mvc, obra, "R-1");
    }

    private ResultActions reservar(long obraId) throws Exception {
        return Publico.reservar(mvc, ip, obraId, cpf, NASCIMENTO);
    }

    private String reservarERetornarProtocolo(long obraId) throws Exception {
        String corpo = reservar(obraId).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.protocolo");
    }

    private long idDaReserva(String protocolo) {
        em.flush();
        return jdbc.queryForObject("select id from reserva where protocolo = ?", Long.class, protocolo);
    }

    private ResultActions emprestar(long leitorId, String tombo) throws Exception {
        return mvc.perform(post("/api/v1/emprestimos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"leitorId\": " + leitorId + ", \"tombo\": \"" + tombo + "\"}"));
    }

    @Test
    void reservaPrendeOExemplarEMostraSoOPrimeiroNome() throws Exception {
        String corpo = reservar(obra)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo").value(Matchers.matchesPattern("[A-HJ-NP-Z2-9]{8}")))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETIRADA"))
                .andExpect(jsonPath("$.titulo").value("Vidas Secas"))
                .andExpect(jsonPath("$.primeiroNome").value("Joana"))
                .andExpect(jsonPath("$.prazoRetirada").value(calendario.somarDiasUteis(hoje, 2).toString()))
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContain(cpf).doesNotContain("Prado").doesNotContain("R-1");

        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("RESERVADO"));
        mvc.perform(get("/api/v1/leitores/" + leitor + "/situacao"))
                .andExpect(jsonPath("$.reservasAguardando").value(1))
                .andExpect(jsonPath("$.livrosEmUso").value(1));
    }

    @Test
    void dadosQueNaoConferemTemMensagemUnica() throws Exception {
        Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO.plusDays(1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(ReservaService.NAO_CONFERE));
        Publico.reservar(mvc, ip, obra, Dados.cpf(), NASCIMENTO)
                .andExpect(jsonPath("$.detail").value(ReservaService.NAO_CONFERE));
        Publico.reservar(mvc, ip, obra, "123", NASCIMENTO)
                .andExpect(jsonPath("$.detail").value(ReservaService.NAO_CONFERE));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
    }

    @Test
    void telefoneEObrigatorioEEmailOpcionalMasValido() throws Exception {
        Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("telefone"));
        Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO, "1234", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("telefone"));
        // Formato aceito pelo padrão, mas sem DDD válido (só 9 dígitos).
        Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO, "(51) 9999-999", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Telefone inválido")));
        Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO, "(51) 99999-9999", "nao-e-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("email"));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
    }

    @Test
    void contatoDaReservaApareceParaAGestao() throws Exception {
        String corpo = Publico.reservar(mvc, ip, obra, cpf, NASCIMENTO, "+55 51 98765 4321", "Joana@Exemplo.com")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContain("98765").doesNotContain("exemplo.com");
        String protocolo = JsonPath.read(corpo, "$.protocolo");
        mvc.perform(get("/api/v1/reservas").param("q", protocolo))
                .andExpect(jsonPath("$.itens[0].leitor.telefone").value("(51) 98765-4321"))
                .andExpect(jsonPath("$.itens[0].leitor.email").value("joana@exemplo.com"));
    }

    @Test
    void telefoneComDddNormaliza() {
        assertThat(ReservaService.telefoneComDdd("51987654321")).isEqualTo("(51) 98765-4321");
        assertThat(ReservaService.telefoneComDdd("+55 (51) 3333-4444")).isEqualTo("(51) 3333-4444");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReservaService.telefoneComDdd("(51) 9999-999"))
                .hasMessageContaining("Telefone inválido");
    }

    @Test
    void aceitaCpfComPontuacao() throws Exception {
        String formatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-"
                + cpf.substring(9);
        Publico.reservar(mvc, ip, obra, formatado, NASCIMENTO).andExpect(status().isCreated());
    }

    @Test
    void umaReservaPorObraESoComExemplarDisponivel() throws Exception {
        Dados.exemplar(mvc, obra, "R-2");
        String protocolo = reservarERetornarProtocolo(obra);
        reservar(obra)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString(protocolo)));

        String outroCpf = Dados.cpf();
        Dados.leitor(mvc, "Pedro", outroCpf, NASCIMENTO);
        Publico.reservar(mvc, ip, obra, outroCpf, NASCIMENTO).andExpect(status().isCreated());

        String terceiro = Dados.cpf();
        Dados.leitor(mvc, "Ana", terceiro, NASCIMENTO);
        Publico.reservar(mvc, ip, obra, terceiro, NASCIMENTO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Não há exemplar disponível")));
    }

    @Test
    void reservasContamNoLimiteDoLeitor() throws Exception {
        long outra = Dados.obra(mvc, Dados.isbn(801), "São Bernardo");
        Dados.exemplar(mvc, outra, "R-10");
        long terceira = Dados.obra(mvc, Dados.isbn(802), "Angústia");
        Dados.exemplar(mvc, terceira, "R-20");
        emprestar(leitor, "R-10").andExpect(status().isCreated());
        emprestar(leitor, "R-20").andExpect(status().isCreated());
        reservarERetornarProtocolo(obra);

        long quarta = Dados.obra(mvc, Dados.isbn(803), "Caetés");
        Dados.exemplar(mvc, quarta, "R-30");
        reservar(quarta)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Você já está com 3 de 3 livros permitidos, somando empréstimos e reservas."));
        emprestar(leitor, "R-30")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Já está com 3 de 3 livros permitidos (contando as reservas)."));
    }

    @Test
    void leitorBloqueadoNaoReserva() throws Exception {
        em.flush();
        em.clear();
        jdbc.update("update leitor set bloqueado_ate = ? where id = ?", hoje.plusDays(3), leitor);
        reservar(obra)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Seu cadastro está bloqueado até")));
    }

    @Test
    void consultaECancelamentoPeloLeitor() throws Exception {
        String protocolo = reservarERetornarProtocolo(obra);
        mvc.perform(postJson("/api/v1/publico/reservas/consulta", identificacao(cpf, NASCIMENTO), ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primeiroNome").value("Joana"))
                .andExpect(jsonPath("$.reservas[0].protocolo").value(protocolo))
                .andExpect(jsonPath("$.reservas[0].status").value("AGUARDANDO_RETIRADA"));

        String outroCpf = Dados.cpf();
        Dados.leitor(mvc, "Pedro", outroCpf, NASCIMENTO);
        mvc.perform(postJson("/api/v1/publico/reservas/" + protocolo + "/cancelamento",
                identificacao(outroCpf, NASCIMENTO), ip)).andExpect(status().isNotFound());

        mvc.perform(postJson("/api/v1/publico/reservas/" + protocolo.toLowerCase() + "/cancelamento",
                identificacao(cpf, NASCIMENTO), ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        mvc.perform(postJson("/api/v1/publico/reservas/" + protocolo + "/cancelamento",
                identificacao(cpf, NASCIMENTO), ip))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Esta reserva foi cancelada."));
    }

    @Test
    void gestaoListaEEntregaComoEmprestimo() throws Exception {
        String protocolo = reservarERetornarProtocolo(obra);
        long id = idDaReserva(protocolo);
        mvc.perform(get("/api/v1/reservas").param("q", protocolo))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].exemplar.tombo").value("R-1"))
                .andExpect(jsonPath("$.itens[0].leitor.nome").value("Joana Prado Lima"))
                .andExpect(jsonPath("$.itens[0].prazoVencido").value(false));
        mvc.perform(get("/api/v1/reservas").param("q", "vidas secas")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/reservas").param("q", protocolo.substring(0, 4).toLowerCase() + "-" + protocolo.substring(4)))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/reservas").param("q", "r-1")).andExpect(jsonPath("$.total").value(1));

        String corpo = mvc.perform(post("/api/v1/reservas/" + id + "/retirada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRADA"))
                .andExpect(jsonPath("$.emprestimoId").isNumber())
                .andExpect(jsonPath("$.devolucaoPrevista").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long emprestimo = ((Number) JsonPath.read(corpo, "$.emprestimoId")).longValue();
        mvc.perform(get("/api/v1/emprestimos/" + emprestimo))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.exemplar.tombo").value("R-1"));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("EMPRESTADO"));
        mvc.perform(get("/api/v1/leitores/" + leitor + "/situacao"))
                .andExpect(jsonPath("$.reservasAguardando").value(0))
                .andExpect(jsonPath("$.livrosEmUso").value(1));

        mvc.perform(get("/api/v1/reservas").param("q", protocolo)).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/reservas").param("status", "RETIRADA", "CANCELADA", "EXPIRADA").param("q", protocolo))
                .andExpect(jsonPath("$.itens[0].status").value("RETIRADA"));
        mvc.perform(post("/api/v1/reservas/" + id + "/retirada"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Esta reserva já foi retirada."));
    }

    @Test
    void limiteNaoImpedeARetiradaMasBloqueioImpede() throws Exception {
        long outra = Dados.obra(mvc, Dados.isbn(804), "Infância");
        Dados.exemplar(mvc, outra, "R-40");
        Dados.exemplar(mvc, outra, "R-41");
        emprestar(leitor, "R-40").andExpect(status().isCreated());
        emprestar(leitor, "R-41").andExpect(status().isCreated());
        long id = idDaReserva(reservarERetornarProtocolo(obra));
        mvc.perform(post("/api/v1/reservas/" + id + "/retirada")).andExpect(status().isOk());

        String outroCpf = Dados.cpf();
        long outroLeitor = Dados.leitor(mvc, "Pedro", outroCpf, NASCIMENTO);
        long obra2 = Dados.obra(mvc, Dados.isbn(805), "Memórias do Cárcere");
        Dados.exemplar(mvc, obra2, "R-50");
        String protocolo = JsonPath.read(Publico.reservar(mvc, ip, obra2, outroCpf, NASCIMENTO)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.protocolo");
        em.flush();
        em.clear();
        jdbc.update("update leitor set bloqueado_ate = ? where id = ?", hoje.plusDays(2), outroLeitor);
        mvc.perform(post("/api/v1/reservas/" + idDaReserva(protocolo) + "/retirada"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Leitor bloqueado até")));
    }

    @Test
    void tomboReservadoBipadoNoBalcaoViraRetirada() throws Exception {
        String protocolo = reservarERetornarProtocolo(obra);
        long outro = Dados.leitor(mvc, "Outro Leitor");
        emprestar(outro, "R-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "O exemplar R-1 está reservado para outro leitor e não pode ser emprestado."));

        emprestar(leitor, "R-1").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ATIVO"));
        em.flush();
        assertThat(jdbc.queryForObject("select status from reserva where protocolo = ?", String.class, protocolo))
                .isEqualTo("RETIRADA");
        assertThat(jdbc.queryForObject("select emprestimo_id from reserva where protocolo = ?", Long.class,
                protocolo)).isNotNull();
    }

    @Test
    void cancelamentoPelaBibliotecaFicaNaAuditoria() throws Exception {
        long id = idDaReserva(reservarERetornarProtocolo(obra));
        mvc.perform(post("/api/v1/reservas/" + id + "/cancelamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.canceladaPor").value("BIBLIOTECA"));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        mvc.perform(get("/api/v1/auditoria").param("acao", "RESERVA_CANCELADA"))
                .andExpect(jsonPath("$.itens[0].entidadeId").value(id))
                .andExpect(jsonPath("$.itens[0].detalhe").value(Matchers.containsString("Vidas Secas")));
    }

    @Test
    void reservaNaoRetiradaNoPrazoExpira() throws Exception {
        String protocolo = reservarERetornarProtocolo(obra);
        long id = idDaReserva(protocolo);
        em.clear();
        jdbc.update("update reserva set prazo_retirada = ? where id = ?", hoje, id);
        assertThat(servico.expirarVencidas()).isZero();

        jdbc.update("update reserva set prazo_retirada = ? where id = ?", hoje.minusDays(1), id);
        mvc.perform(get("/api/v1/reservas/" + id)).andExpect(jsonPath("$.prazoVencido").value(true));
        em.clear();
        assertThat(servico.expirarVencidas()).isEqualTo(1);
        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/reservas/" + id)).andExpect(jsonPath("$.status").value("EXPIRADA"));
        mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        mvc.perform(post("/api/v1/reservas/" + id + "/retirada"))
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Esta reserva expirou")));
    }

    @Test
    void reservadoNaoMudaPelaTelaDoExemplar() throws Exception {
        reservarERetornarProtocolo(obra);
        String corpo = mvc.perform(get("/api/v1/exemplares/tombo/R-1")).andReturn().getResponse().getContentAsString();
        long exemplar = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/exemplares/" + exemplar).contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\": \"BAIXADO\"}"))
                .andExpect(status().isUnprocessableContent());
    }
}
