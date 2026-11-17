package br.gov.imbe.biblioteca.importacao;

import java.util.List;

import br.gov.imbe.biblioteca.acervo.ObraRequisicao;
import br.gov.imbe.biblioteca.isbn.DadosLivro;

/**
 * Converte os dados da fonte no cadastro da obra, respeitando os limites que o formulário exige do funcionário
 * (no lote ninguém confere antes: texto longo demais é cortado, ano ou páginas fora da faixa ficam vazios).
 */
final class ObraDaFonte {

    private ObraDaFonte() {
    }

    static ObraRequisicao de(DadosLivro d) {
        return new ObraRequisicao(d.isbn13(), cortar(d.titulo(), 500), cortar(d.subtitulo(), 500),
                lista(d.autores(), 20, 255), cortar(d.editora(), 255),
                d.ano() != null && d.ano() >= 1400 && d.ano() <= 2100 ? d.ano() : null,
                d.paginas() != null && d.paginas() <= 99999 ? d.paginas() : null,
                d.idioma() != null && d.idioma().length() <= 10 ? d.idioma() : null,
                cortar(d.sinopse(), 20000), lista(d.assuntos(), 30, 150), d.fonte());
    }

    private static String cortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max).strip();
    }

    private static List<String> lista(List<String> itens, int maximo, int tamanho) {
        return itens.stream().limit(maximo).map(s -> cortar(s, tamanho)).toList();
    }
}
