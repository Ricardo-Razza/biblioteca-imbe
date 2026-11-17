package br.gov.imbe.biblioteca.importacao;

import java.time.LocalDateTime;
import java.util.Map;

/** Respostas da importação na API. */
public final class ImportacaoDtos {

    private ImportacaoDtos() {
    }

    /**
     * @param contagem quantos itens em cada resultado (todos os resultados aparecem, com zero se for o caso)
     * @param pendentes ainda na fila; zero quando concluída
     */
    public record ImportacaoResposta(Long id, String arquivo, String funcionario, LocalDateTime criadaEm,
            Importacao.Status status, LocalDateTime finalizadaEm, long total, long pendentes,
            Map<ResultadoItem, Long> contagem) {
    }

    public record ItemResposta(Long id, int linha, String isbnOriginal, String isbn13, String tombo,
            String localizacao, ResultadoItem resultado, Long obraId, String titulo, String mensagem) {
    }
}
