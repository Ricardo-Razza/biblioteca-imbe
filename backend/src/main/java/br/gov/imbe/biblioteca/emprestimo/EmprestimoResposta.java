package br.gov.imbe.biblioteca.emprestimo;

import java.time.LocalDate;
import java.util.List;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.leitor.Leitor;

/**
 * Empréstimo na gestão.
 *
 * @param renovacoesRestantes 0 quando já devolvido
 * @param bloqueadoAte        só na resposta da devolução com atraso: até quando o leitor fica bloqueado
 */
public record EmprestimoResposta(
        Long id,
        ExemplarResumo exemplar,
        LeitorResumo leitor,
        LocalDate dataSaida,
        LocalDate dataPrevista,
        LocalDate dataDevolucao,
        int renovacoes,
        int renovacoesRestantes,
        long diasAtraso,
        StatusEmprestimo status,
        OcorrenciaDevolucao ocorrenciaDevolucao,
        String observacao,
        LocalDate bloqueadoAte) {

    public record ExemplarResumo(Long id, String tombo, Long obraId, String titulo, List<String> autores,
            boolean obraTemCapa) {
    }

    public record LeitorResumo(Long id, String nome) {
    }

    static EmprestimoResposta de(Emprestimo e, LocalDate hoje, int maximoRenovacoes) {
        return de(e, hoje, maximoRenovacoes, null);
    }

    static EmprestimoResposta de(Emprestimo e, LocalDate hoje, int maximoRenovacoes, LocalDate bloqueadoAte) {
        Exemplar x = e.getExemplar();
        Leitor l = e.getLeitor();
        Obra obra = x.getObra();
        return new EmprestimoResposta(e.getId(),
                new ExemplarResumo(x.getId(), x.getTombo(), obra.getId(), obra.getTitulo(),
                        obra.getAutores().stream().map(oa -> oa.getAutor().getNome()).toList(),
                        obra.getCapaArquivo() != null),
                new LeitorResumo(l.getId(), l.getNome()),
                e.getDataSaida(), e.getDataPrevista(), e.getDataDevolucao(), e.getRenovacoes(),
                e.emAberto() ? Math.max(0, maximoRenovacoes - e.getRenovacoes()) : 0,
                e.diasAtraso(hoje), e.status(hoje), e.getOcorrenciaDevolucao(), e.getObservacao(), bloqueadoAte);
    }
}
