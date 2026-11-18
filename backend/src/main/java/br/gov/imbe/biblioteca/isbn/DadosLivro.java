package br.gov.imbe.biblioteca.isbn;

import java.util.List;

/**
 * Dados de um livro como vieram de uma fonte externa, já normalizados: textos sem espaços nas pontas,
 * vazios viram {@code null}, listas nunca são nulas. Não é salvo direto: o funcionário confere antes.
 *
 * @param capas endereços externos de capa, na ordem em que vale a pena tentar; usados só para baixar, nunca vão
 *              para a tela. Vários porque uma fonte pode indicar uma capa que não existe ou é só um desenho de
 *              "sem imagem".
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
        List<String> capas,
        FonteIsbn fonte) {

    public DadosLivro {
        titulo = texto(titulo);
        subtitulo = texto(subtitulo);
        editora = texto(editora);
        idioma = texto(idioma);
        sinopse = texto(sinopse);
        capas = lista(capas);
        autores = lista(autores);
        assuntos = lista(assuntos);
        ano = positivo(ano);
        paginas = positivo(paginas);
    }

    /** Os mesmos dados com outra lista de capas (a busca reordena as capas de todas as fontes). */
    public DadosLivro comCapas(List<String> novas) {
        return new DadosLivro(isbn13, titulo, subtitulo, autores, editora, ano, paginas, idioma, sinopse, assuntos,
                novas, fonte);
    }

    /**
     * Mescla: estes dados mandam e {@code outra} só completa o que está vazio; as capas somam (estas primeiro).
     * A fonte continua sendo a desta.
     */
    public DadosLivro completadoCom(DadosLivro outra) {
        return new DadosLivro(
                isbn13,
                titulo != null ? titulo : outra.titulo,
                subtitulo != null ? subtitulo : outra.subtitulo,
                autores.isEmpty() ? outra.autores : autores,
                editora != null ? editora : outra.editora,
                ano != null ? ano : outra.ano,
                paginas != null ? paginas : outra.paginas,
                idioma != null ? idioma : outra.idioma,
                sinopse != null ? sinopse : outra.sinopse,
                assuntos.isEmpty() ? outra.assuntos : assuntos,
                java.util.stream.Stream.concat(capas.stream(), outra.capas.stream()).toList(),
                fonte);
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
