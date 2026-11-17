package br.gov.imbe.biblioteca.isbn;

import java.util.List;
import java.util.Optional;

/**
 * Resultado de uma busca nas fontes.
 *
 * @param dados                dados mesclados, ou vazio se nenhuma fonte conhece o ISBN
 * @param fontesComDados       fontes que contribuíram, na ordem de prioridade
 * @param fontesIndisponiveis  fontes que falharam; se houver alguma, o resultado não vai para o cache
 */
public record ResultadoIsbn(
        Isbn isbn,
        Optional<DadosLivro> dados,
        List<FonteIsbn> fontesComDados,
        List<FonteIsbn> fontesIndisponiveis) {

    public ResultadoIsbn {
        fontesComDados = List.copyOf(fontesComDados);
        fontesIndisponiveis = List.copyOf(fontesIndisponiveis);
    }

    public boolean encontrado() {
        return dados.isPresent();
    }

    /** Resposta completa: todas as fontes responderam (com ou sem o livro). */
    public boolean completo() {
        return fontesIndisponiveis.isEmpty();
    }
}
