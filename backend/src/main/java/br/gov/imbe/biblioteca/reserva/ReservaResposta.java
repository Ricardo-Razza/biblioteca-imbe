package br.gov.imbe.biblioteca.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.gov.imbe.biblioteca.acervo.Exemplar;
import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.leitor.Leitor;

/**
 * Reserva na gestão: o que separar (tombo e localização) e para quem.
 *
 * @param prazoVencido      aguardando com o prazo já passado (o job ainda não expirou; ainda dá para entregar)
 * @param devolucaoPrevista só quando retirada: data prevista do empréstimo aberto na retirada
 */
public record ReservaResposta(
        Long id,
        String protocolo,
        StatusReserva status,
        ObraResumo obra,
        ExemplarResumo exemplar,
        LeitorResumo leitor,
        LocalDateTime criadaEm,
        LocalDate prazoRetirada,
        boolean prazoVencido,
        LocalDateTime finalizadaEm,
        CanceladaPor canceladaPor,
        Long emprestimoId,
        LocalDate devolucaoPrevista) {

    public record ObraResumo(Long id, String titulo, List<String> autores, boolean temCapa) {
    }

    public record ExemplarResumo(Long id, String tombo, String localizacao) {
    }

    /**
     * Telefone e e-mail informados na reserva; sem eles (reserva antiga), os do cadastro do leitor.
     *
     * @param cadastroPendente pré-cadastro feito pelo site: conferir o documento antes de entregar
     */
    public record LeitorResumo(Long id, String nome, String telefone, String email, boolean cadastroPendente) {
    }

    static ReservaResposta de(Reserva r, LocalDate hoje) {
        Obra o = r.getObra();
        Exemplar x = r.getExemplar();
        Leitor l = r.getLeitor();
        return new ReservaResposta(r.getId(), r.getProtocolo(), r.getStatus(),
                new ObraResumo(o.getId(), o.getTitulo(), autores(o), o.getCapaArquivo() != null),
                new ExemplarResumo(x.getId(), x.getTombo(), x.getLocalizacao()),
                new LeitorResumo(l.getId(), l.getNome(),
                        r.getTelefone() != null ? r.getTelefone() : l.getTelefone(),
                        r.getEmail() != null ? r.getEmail() : l.getEmail(), l.isCadastroPendente()),
                r.getCriadaEm(), r.getPrazoRetirada(), r.aguardando() && r.getPrazoRetirada().isBefore(hoje),
                r.getFinalizadaEm(), r.getCanceladaPor(),
                r.getEmprestimo() == null ? null : r.getEmprestimo().getId(),
                r.getEmprestimo() == null ? null : r.getEmprestimo().getDataPrevista());
    }

    static List<String> autores(Obra o) {
        return o.getAutores().stream().map(oa -> oa.getAutor().getNome()).toList();
    }
}
