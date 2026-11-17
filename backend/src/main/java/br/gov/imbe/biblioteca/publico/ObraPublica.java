package br.gov.imbe.biblioteca.publico;

import java.util.Comparator;
import java.util.List;

import br.gov.imbe.biblioteca.acervo.Assunto;
import br.gov.imbe.biblioteca.acervo.Obra;

/**
 * Obra no site público: só dados da obra e quantos exemplares estão disponíveis. Nunca tombo, leitor ou empréstimo.
 * A capa sai por {@code GET /api/v1/obras/{id}/capa}, que é público.
 */
public record ObraPublica(
        Long id,
        String titulo,
        String subtitulo,
        List<String> autores,
        Integer ano,
        boolean temCapa,
        int disponiveis) {

    static ObraPublica de(Obra o) {
        return new ObraPublica(o.getId(), o.getTitulo(), o.getSubtitulo(), autores(o),
                o.getAno() == null ? null : o.getAno().intValue(), o.getCapaArquivo() != null,
                o.getExemplaresDisponiveis());
    }

    static List<String> autores(Obra o) {
        return o.getAutores().stream().map(oa -> oa.getAutor().getNome()).toList();
    }

    /** Ficha do livro. */
    public record Detalhe(
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
            List<AssuntoDaObra> assuntos,
            boolean temCapa,
            int disponiveis) {

        static Detalhe de(Obra o) {
            return new Detalhe(o.getId(), o.getIsbn13(), o.getTitulo(), o.getSubtitulo(), ObraPublica.autores(o), o.getEditora(),
                    o.getAno() == null ? null : o.getAno().intValue(), o.getPaginas(), o.getIdioma(), o.getSinopse(),
                    o.getAssuntos().stream().sorted(Comparator.comparing(Assunto::getNome))
                            .map(a -> new AssuntoDaObra(a.getId(), a.getNome())).toList(),
                    o.getCapaArquivo() != null, o.getExemplaresDisponiveis());
        }
    }

    public record AssuntoDaObra(Long id, String nome) {
    }
}
