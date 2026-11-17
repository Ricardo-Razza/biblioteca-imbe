package br.gov.imbe.biblioteca.isbn;

import java.util.List;

/**
 * Resposta de {@code GET /api/v1/isbn/{isbn}}. Não salva nada: o front mostra o formulário preenchido para conferência.
 * A URL externa da capa não sai daqui (a tela nunca usa link externo); {@code capaDisponivel} diz se há capa para
 * baixar ao salvar a obra.
 *
 * @param encontrado          falso: abrir o formulário manual
 * @param fontesIndisponiveis fontes que falharam; com {@code encontrado = false}, avisar que pode valer tentar de novo
 */
public record IsbnResposta(
        String isbn13,
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

    static IsbnResposta de(ResultadoIsbn r) {
        Dados dados = r.dados()
                .map(d -> new Dados(d.titulo(), d.subtitulo(), d.autores(), d.editora(), d.ano(), d.paginas(),
                        d.idioma(), d.sinopse(), d.assuntos(), d.capaUrl() != null, d.fonte()))
                .orElse(null);
        return new IsbnResposta(r.isbn().valor(), r.encontrado(), dados, r.fontesComDados(), r.fontesIndisponiveis());
    }
}
