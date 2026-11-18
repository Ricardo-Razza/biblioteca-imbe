package br.gov.imbe.biblioteca.isbn;

import java.util.List;

/**
 * Resposta de {@code GET /api/v1/isbn/{isbn}}. Não salva nada: o front mostra o formulário preenchido para conferência.
 * A URL externa da capa não sai daqui (a tela nunca usa link externo); {@code capaDisponivel} diz se há capa para
 * baixar ao salvar a obra.
 *
 * @param obraId              preenchido quando a obra já está no acervo: a tela só oferece "Adicionar exemplar"
 *                            e as fontes externas nem são consultadas
 * @param encontrado          falso (e sem {@code obraId}): abrir o formulário manual
 * @param fontesIndisponiveis fontes que falharam; com {@code encontrado = false}, avisar que pode valer tentar de novo
 */
public record IsbnResposta(
        String isbn13,
        Long obraId,
        boolean encontrado,
        Dados dados,
        List<FonteIsbn> fontes,
        List<FonteIsbn> fontesIndisponiveis) {

    public record Dados(
            String titulo,
            String subtitulo,
            List<String> autores,
            String editora,
            Integer ano,
            Integer paginas,
            String idioma,
            String sinopse,
            List<String> assuntos,
            boolean capaDisponivel,
            FonteIsbn fonte) {
    }

    static IsbnResposta jaCadastrada(Isbn isbn, Long obraId) {
        return new IsbnResposta(isbn.valor(), obraId, false, null, List.of(), List.of());
    }

    /** @param capaDisponivel se alguma das capas indicadas foi baixada e é uma capa de verdade */
    static IsbnResposta de(ResultadoIsbn r, boolean capaDisponivel) {
        Dados dados = r.dados()
                .map(d -> new Dados(d.titulo(), d.subtitulo(), d.autores(), d.editora(), d.ano(), d.paginas(),
                        d.idioma(), d.sinopse(), d.assuntos(), capaDisponivel, d.fonte()))
                .orElse(null);
        return new IsbnResposta(r.isbn().valor(), null, r.encontrado(), dados, r.fontesComDados(),
                r.fontesIndisponiveis());
    }
}
