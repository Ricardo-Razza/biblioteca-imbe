package br.gov.imbe.biblioteca.acervo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.gov.imbe.biblioteca.isbn.FonteIsbn;

/** Obra na API da gestão. A capa sai pelo endpoint próprio ({@code GET /obras/{id}/capa}), não como arquivo. */
public record ObraResposta(
        Long id,
        String isbn13,
        String titulo,
        String subtitulo,
        List<String> autores,
        String editora,
        Integer ano,
        Integer paginas,
        String idioma,
        String sinopse,
        List<String> assuntos,
        boolean temCapa,
        FonteIsbn fonteDados,
        LocalDateTime criadoEm) {

    static ObraResposta de(Obra o) {
        return new ObraResposta(
                o.getId(),
                o.getIsbn13(),
                o.getTitulo(),
                o.getSubtitulo(),
                o.getAutores().stream().map(oa -> oa.getAutor().getNome()).toList(),
                o.getEditora(),
                o.getAno() == null ? null : o.getAno().intValue(),
                o.getPaginas(),
                o.getIdioma(),
                o.getSinopse(),
                o.getAssuntos().stream().map(Assunto::getNome).sorted(Comparator.naturalOrder()).toList(),
                o.getCapaArquivo() != null,
                o.getFonteDados(),
                o.getCriadoEm());
    }
}
