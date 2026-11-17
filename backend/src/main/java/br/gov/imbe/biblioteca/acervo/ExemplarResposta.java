package br.gov.imbe.biblioteca.acervo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Exemplar com um resumo da obra, para a busca por tombo no balcão. */
public record ExemplarResposta(
        Long id,
        String tombo,
        String localizacao,
        EstadoExemplar estado,
        SituacaoExemplar situacao,
        LocalDate dataAquisicao,
        OrigemExemplar origem,
        LocalDateTime criadoEm,
        ObraResumo obra) {

    public record ObraResumo(Long id, String isbn13, String titulo, List<String> autores) {
    }

    static ExemplarResposta de(Exemplar e) {
        Obra o = e.getObra();
        return new ExemplarResposta(e.getId(), e.getTombo(), e.getLocalizacao(), e.getEstado(), e.getSituacao(),
                e.getDataAquisicao(), e.getOrigem(), e.getCriadoEm(),
                new ObraResumo(o.getId(), o.getIsbn13(), o.getTitulo(),
                        o.getAutores().stream().map(oa -> oa.getAutor().getNome()).toList()));
    }
}
