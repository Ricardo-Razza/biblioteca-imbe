package br.gov.imbe.biblioteca.relatorio;

import java.time.LocalDate;
import java.util.List;

/**
 * Tela inicial da gestão: o que precisa de atenção hoje e o movimento da última semana.
 * As listas trazem no máximo {@value PainelService#ITENS_POR_LISTA} itens; os totais estão em {@code contadores}.
 */
public record PainelResposta(
        LocalDate hoje,
        Contadores contadores,
        List<EmprestimoItem> venceHoje,
        List<EmprestimoItem> atrasados,
        List<ReservaItem> paraSeparar,
        List<Dia> movimento) {

    /** @param emprestados empréstimos em aberto, atrasados inclusive */
    public record Contadores(long emprestados, long atrasados, long venceHoje, long reservasParaSeparar,
            long leitoresBloqueados) {
    }

    public record EmprestimoItem(Long id, String tombo, Long obraId, String titulo, Long leitorId, String leitor,
            String telefone, LocalDate dataPrevista, long diasAtraso) {
    }

    public record ReservaItem(Long id, String protocolo, Long obraId, String titulo, String tombo,
            String localizacao, Long leitorId, String leitor, LocalDate prazoRetirada) {
    }

    /** Saídas e devoluções de um dia. */
    public record Dia(LocalDate data, long saidas, long devolucoes) {
    }
}
