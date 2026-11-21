package br.gov.imbe.biblioteca.relatorio;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.gov.imbe.biblioteca.relatorio.PainelResposta.Contadores;
import br.gov.imbe.biblioteca.relatorio.PainelResposta.EmprestimoItem;
import br.gov.imbe.biblioteca.relatorio.PainelResposta.ReservaItem;

/**
 * Consultas de leitura do painel e dos relatórios, em SQL direto: juntam várias tabelas e agregam, sem passar
 * pelas entidades. Os relatórios entregam linha a linha ao {@link RowCallbackHandler}, sem montar lista.
 */
@Repository
class RelatorioRepository {

    private static final String AUTORES = """
            (select group_concat(a.nome order by oa.ordem separator ', ')
               from obra_autor oa join autor a on a.id = oa.autor_id where oa.obra_id = o.id)""";

    private static final String EMPRESTIMO_ITEM = """
            select e.id, x.tombo, o.id obra_id, o.titulo, l.id leitor_id, l.nome, l.telefone, e.data_prevista
              from emprestimo e
              join exemplar x on x.id = e.exemplar_id
              join obra o on o.id = x.obra_id
              join leitor l on l.id = e.leitor_id
            """;

    private final JdbcClient jdbc;

    RelatorioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Contadores contadores(LocalDate hoje) {
        return jdbc.sql("""
                select
                  (select count(*) from emprestimo where data_devolucao is null) emprestados,
                  (select count(*) from emprestimo where data_devolucao is null and data_prevista < :hoje) atrasados,
                  (select count(*) from emprestimo where data_devolucao is null and data_prevista = :hoje) vence_hoje,
                  (select count(*) from reserva where status = 'AGUARDANDO_RETIRADA') para_separar,
                  (select count(*) from leitor where ativo and bloqueado_ate >= :hoje) bloqueados
                """).param("hoje", hoje)
                .query((rs, n) -> new Contadores(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                        rs.getLong(5)))
                .single();
    }

    List<EmprestimoItem> venceHoje(LocalDate hoje, int limite) {
        return jdbc.sql(EMPRESTIMO_ITEM + """
                 where e.data_devolucao is null and e.data_prevista = :hoje
                 order by l.nome, e.id limit :limite
                """).param("hoje", hoje).param("limite", limite).query((rs, n) -> item(rs, hoje)).list();
    }

    List<EmprestimoItem> atrasados(LocalDate hoje, int limite) {
        return jdbc.sql(EMPRESTIMO_ITEM + """
                 where e.data_devolucao is null and e.data_prevista < :hoje
                 order by e.data_prevista, e.id limit :limite
                """).param("hoje", hoje).param("limite", limite).query((rs, n) -> item(rs, hoje)).list();
    }

    private static EmprestimoItem item(ResultSet rs, LocalDate hoje) throws SQLException {
        LocalDate prevista = data(rs, "data_prevista");
        return new EmprestimoItem(rs.getLong("id"), rs.getString("tombo"), rs.getLong("obra_id"),
                rs.getString("titulo"), rs.getLong("leitor_id"), rs.getString("nome"), rs.getString("telefone"),
                prevista, Math.max(0, ChronoUnit.DAYS.between(prevista, hoje)));
    }

    List<ReservaItem> paraSeparar(int limite) {
        return jdbc.sql("""
                select r.id, r.protocolo, o.id obra_id, o.titulo, x.tombo, x.localizacao, l.id leitor_id, l.nome,
                       r.prazo_retirada
                  from reserva r
                  join obra o on o.id = r.obra_id
                  join exemplar x on x.id = r.exemplar_id
                  join leitor l on l.id = r.leitor_id
                 where r.status = 'AGUARDANDO_RETIRADA'
                 order by r.prazo_retirada, r.id limit :limite
                """).param("limite", limite)
                .query((rs, n) -> new ReservaItem(rs.getLong("id"), rs.getString("protocolo"), rs.getLong("obra_id"),
                        rs.getString("titulo"), rs.getString("tombo"), rs.getString("localizacao"),
                        rs.getLong("leitor_id"), rs.getString("nome"), data(rs, "prazo_retirada")))
                .list();
    }

    /** Saídas por dia entre {@code de} e {@code ate}, inclusive. Dia sem movimento fica fora do mapa. */
    Map<LocalDate, Long> saidasPorDia(LocalDate de, LocalDate ate) {
        return porDia("data_saida", de, ate);
    }

    Map<LocalDate, Long> devolucoesPorDia(LocalDate de, LocalDate ate) {
        return porDia("data_devolucao", de, ate);
    }

    private Map<LocalDate, Long> porDia(String coluna, LocalDate de, LocalDate ate) {
        // coluna vem só das duas chamadas acima, nunca da requisição
        return jdbc.sql("select " + coluna + ", count(*) from emprestimo where " + coluna
                + " between :de and :ate group by " + coluna)
                .param("de", de).param("ate", ate)
                .query((rs, n) -> Map.entry(rs.getDate(1).toLocalDate(), rs.getLong(2))).list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // ---- relatórios ----

    void emprestimos(LocalDate de, LocalDate ate, RowCallbackHandler linha) {
        jdbc.sql("""
                select e.data_saida, e.data_prevista, e.data_devolucao, e.renovacoes, e.ocorrencia_devolucao,
                       x.tombo, o.titulo, l.nome leitor
                  from emprestimo e
                  join exemplar x on x.id = e.exemplar_id
                  join obra o on o.id = x.obra_id
                  join leitor l on l.id = e.leitor_id
                 where e.data_saida between :de and :ate
                 order by e.data_saida, e.id
                """).param("de", de).param("ate", ate).query(linha);
    }

    void atrasados(LocalDate hoje, RowCallbackHandler linha) {
        jdbc.sql("""
                select x.tombo, o.titulo, l.nome leitor, l.telefone, l.email, e.data_saida, e.data_prevista
                  from emprestimo e
                  join exemplar x on x.id = e.exemplar_id
                  join obra o on o.id = x.obra_id
                  join leitor l on l.id = e.leitor_id
                 where e.data_devolucao is null and e.data_prevista < :hoje
                 order by e.data_prevista, e.id
                """).param("hoje", hoje).query(linha);
    }

    void acervo(RowCallbackHandler linha) {
        jdbc.sql("""
                select x.tombo, o.isbn13, o.titulo, %s autores, o.editora, o.ano, x.localizacao, x.estado,
                       x.situacao, x.data_aquisicao, x.origem
                  from exemplar x
                  join obra o on o.id = x.obra_id
                 order by o.titulo, x.tombo
                """.formatted(AUTORES)).query(linha);
    }

    void maisEmprestados(LocalDate de, LocalDate ate, int limite, RowCallbackHandler linha) {
        jdbc.sql("""
                select o.titulo, %s autores, o.isbn13, count(*) total
                  from emprestimo e
                  join exemplar x on x.id = e.exemplar_id
                  join obra o on o.id = x.obra_id
                 where e.data_saida between :de and :ate
                 group by o.id, o.titulo, o.isbn13
                 order by total desc, o.titulo
                 limit :limite
                """.formatted(AUTORES)).param("de", de).param("ate", ate).param("limite", limite).query(linha);
    }

    void reservas(LocalDate de, LocalDate ate, RowCallbackHandler linha) {
        jdbc.sql("""
                select r.protocolo, r.criada_em, o.titulo, x.tombo, l.nome leitor, r.status, r.prazo_retirada,
                       r.finalizada_em, r.cancelada_por
                  from reserva r
                  join obra o on o.id = r.obra_id
                  join exemplar x on x.id = r.exemplar_id
                  join leitor l on l.id = r.leitor_id
                 where r.criada_em >= :de and r.criada_em < :depois
                 order by r.criada_em, r.id
                """).param("de", de).param("depois", ate.plusDays(1)).query(linha);
    }

    /** Termos que mais vezes não acharam nada no período, com o dia da última vez. */
    void buscasSemResultado(LocalDate de, LocalDate ate, int limite, RowCallbackHandler linha) {
        jdbc.sql("""
                select termo, sum(vezes) total, max(dia) ultima
                  from busca_sem_resultado
                 where dia between :de and :ate
                 group by termo
                 order by total desc, termo
                 limit :limite
                """).param("de", de).param("ate", ate).param("limite", limite).query(linha);
    }

    static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        Date d = rs.getDate(coluna);
        return d == null ? null : d.toLocalDate();
    }
}
