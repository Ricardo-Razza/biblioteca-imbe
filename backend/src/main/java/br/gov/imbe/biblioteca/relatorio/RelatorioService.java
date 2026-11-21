package br.gov.imbe.biblioteca.relatorio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/** Monta os relatórios em CSV. Cada exportação fica na auditoria (alguns têm dados de leitores). */
@Service
public class RelatorioService {

    /** Período padrão quando {@code de} não vem: os últimos 30 dias. */
    static final int DIAS_PADRAO = 30;
    static final int MAXIMO_DIAS = 366 * 5;
    static final int LIMITE_MAIS_EMPRESTADOS = 100;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RelatorioRepository consultas;
    private final Auditoria auditoria;
    private final Clock relogio;

    public RelatorioService(RelatorioRepository consultas, Auditoria auditoria, Clock relogio) {
        this.consultas = consultas;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public record Arquivo(String nome, byte[] conteudo) {
    }

    @Transactional
    public Arquivo gerar(String caminho, LocalDate de, LocalDate ate) {
        TipoRelatorio tipo = TipoRelatorio.doCaminho(caminho)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório não encontrado: " + caminho + "."));
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate fim = ate == null ? hoje : ate;
        LocalDate inicio = de == null ? fim.minusDays(DIAS_PADRAO - 1) : de;
        if (tipo.periodo) {
            if (inicio.isAfter(fim)) {
                throw new RequisicaoInvalidaException("Período inválido", "A data inicial deve ser anterior ou igual à final.");
            }
            if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DIAS) {
                throw new RequisicaoInvalidaException("Período inválido", "O período pode ter no máximo 5 anos.");
            }
        }

        Csv csv = new Csv();
        switch (tipo) {
            case EMPRESTIMOS -> emprestimos(csv, inicio, fim, hoje);
            case ATRASADOS -> atrasados(csv, hoje);
            case ACERVO -> acervo(csv);
            case MAIS_EMPRESTADOS -> maisEmprestados(csv, inicio, fim);
            case RESERVAS -> reservas(csv, inicio, fim);
        }

        String periodo = tipo.periodo ? " de " + DATA.format(inicio) + " a " + DATA.format(fim) : "";
        auditoria.registrar(AcaoAuditoria.RELATORIO_EXPORTADO, "relatorio", null, tipo.nome + periodo);
        String nome = tipo.periodo
                ? "%s-%s-a-%s.csv".formatted(tipo.caminho, inicio, fim)
                : "%s-%s.csv".formatted(tipo.caminho, hoje);
        return new Arquivo(nome, csv.bytes());
    }

    private void emprestimos(Csv csv, LocalDate de, LocalDate ate, LocalDate hoje) {
        csv.linha("Saída", "Devolução prevista", "Devolvido em", "Situação", "Renovações", "Ocorrência", "Tombo",
                "Título", "Leitor");
        consultas.emprestimos(de, ate, rs -> {
            LocalDate prevista = RelatorioRepository.data(rs, "data_prevista");
            LocalDate devolucao = RelatorioRepository.data(rs, "data_devolucao");
            String situacao = devolucao != null ? "Devolvido" : prevista.isBefore(hoje) ? "Atrasado" : "Em aberto";
            csv.linha(RelatorioRepository.data(rs, "data_saida"), prevista, devolucao, situacao,
                    rs.getInt("renovacoes"), ocorrencia(rs.getString("ocorrencia_devolucao")), rs.getString("tombo"),
                    rs.getString("titulo"), rs.getString("leitor"));
        });
    }

    private void atrasados(Csv csv, LocalDate hoje) {
        csv.linha("Tombo", "Título", "Leitor", "Telefone", "E-mail", "Saída", "Devolução prevista", "Dias de atraso");
        consultas.atrasados(hoje, rs -> {
            LocalDate prevista = RelatorioRepository.data(rs, "data_prevista");
            csv.linha(rs.getString("tombo"), rs.getString("titulo"), rs.getString("leitor"), rs.getString("telefone"),
                    rs.getString("email"), RelatorioRepository.data(rs, "data_saida"), prevista,
                    ChronoUnit.DAYS.between(prevista, hoje));
        });
    }

    private void acervo(Csv csv) {
        csv.linha("Tombo", "ISBN", "Título", "Autores", "Editora", "Ano", "Localização", "Estado", "Situação",
                "Aquisição", "Origem");
        consultas.acervo(rs -> csv.linha(rs.getString("tombo"), rs.getString("isbn13"), rs.getString("titulo"),
                rs.getString("autores"), rs.getString("editora"), inteiro(rs, "ano"), rs.getString("localizacao"),
                estado(rs.getString("estado")), situacaoExemplar(rs.getString("situacao")),
                RelatorioRepository.data(rs, "data_aquisicao"), origem(rs.getString("origem"))));
    }

    private void maisEmprestados(Csv csv, LocalDate de, LocalDate ate) {
        csv.linha("Posição", "Título", "Autores", "ISBN", "Empréstimos");
        int[] posicao = {0};
        consultas.maisEmprestados(de, ate, LIMITE_MAIS_EMPRESTADOS, rs -> csv.linha(++posicao[0],
                rs.getString("titulo"), rs.getString("autores"), rs.getString("isbn13"), rs.getLong("total")));
    }

    private void reservas(Csv csv, LocalDate de, LocalDate ate) {
        csv.linha("Protocolo", "Feita em", "Título", "Tombo", "Leitor", "Situação", "Prazo de retirada",
                "Finalizada em", "Cancelada por");
        consultas.reservas(de, ate, rs -> {
            String p = rs.getString("protocolo");
            csv.linha(p.substring(0, 4) + "-" + p.substring(4), dataHora(rs, "criada_em"), rs.getString("titulo"),
                    rs.getString("tombo"), rs.getString("leitor"), situacaoReserva(rs.getString("status")),
                    RelatorioRepository.data(rs, "prazo_retirada"), dataHora(rs, "finalizada_em"),
                    canceladaPor(rs.getString("cancelada_por")));
        });
    }

    private static Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    private static LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toLocalDateTime();
    }

    static String ocorrencia(String v) {
        return v == null ? null : switch (v) {
            case "DANIFICADO" -> "Danificado";
            case "EXTRAVIADO" -> "Extraviado";
            default -> v;
        };
    }

    static String estado(String v) {
        return v == null ? null : switch (v) {
            case "BOM" -> "Bom";
            case "DANIFICADO" -> "Danificado";
            default -> v;
        };
    }

    static String situacaoExemplar(String v) {
        return switch (v) {
            case "DISPONIVEL" -> "Disponível";
            case "RESERVADO" -> "Reservado";
            case "EMPRESTADO" -> "Emprestado";
            case "EXTRAVIADO" -> "Extraviado";
            case "BAIXADO" -> "Baixado";
            default -> v;
        };
    }

    static String origem(String v) {
        return v == null ? null : switch (v) {
            case "COMPRA" -> "Compra";
            case "DOACAO" -> "Doação";
            default -> v;
        };
    }

    static String situacaoReserva(String v) {
        return switch (v) {
            case "AGUARDANDO_RETIRADA" -> "Aguardando retirada";
            case "RETIRADA" -> "Retirada";
            case "CANCELADA" -> "Cancelada";
            case "EXPIRADA" -> "Expirada";
            default -> v;
        };
    }

    static String canceladaPor(String v) {
        return v == null ? null : switch (v) {
            case "LEITOR" -> "Leitor";
            case "BIBLIOTECA" -> "Sala Literária";
            default -> v;
        };
    }
}
