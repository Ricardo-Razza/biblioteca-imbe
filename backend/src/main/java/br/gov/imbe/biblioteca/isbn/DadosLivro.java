package br.gov.imbe.biblioteca.isbn;

import java.util.List;

/**
 * Dados de um livro como vieram de uma fonte externa, já normalizados: textos sem espaços nas pontas,
 * vazios viram {@code null}, listas nunca são nulas. Não é salvo direto: o funcionário confere antes.
 *
 * @param capaUrl endereço externo da capa, usado só para baixá-la; nunca vai para a tela
 */
public record DadosLivro(
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
        String capaUrl,
        FonteIsbn fonte) {

    public DadosLivro {
        titulo = texto(titulo);
        subtitulo = texto(subtitulo);
        editora = texto(editora);
        idioma = texto(idioma);
        sinopse = texto(sinopse);
        capaUrl = texto(capaUrl);
        autores = lista(autores);
        assuntos = lista(assuntos);
        ano = positivo(ano);
        paginas = positivo(paginas);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static List<String> lista(List<String> valores) {
        return valores == null ? List.of()
                : valores.stream().map(DadosLivro::texto).filter(v -> v != null).distinct().toList();
    }

    private static Integer positivo(Integer valor) {
        return valor == null || valor <= 0 ? null : valor;
    }
}
