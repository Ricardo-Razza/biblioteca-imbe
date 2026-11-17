package br.gov.imbe.biblioteca.acervo;

import java.util.List;

import br.gov.imbe.biblioteca.isbn.FonteIsbn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cadastro ou alteração de obra, depois da conferência do funcionário.
 *
 * @param isbn       ISBN-10 ou ISBN-13, com ou sem hífens (é normalizado para ISBN-13)
 * @param fonteDados de onde vieram os dados; sem valor, {@code MANUAL}
 */
public record ObraRequisicao(
        @NotBlank(message = "Informe o ISBN.") String isbn,
        @NotBlank(message = "Informe o título.") @Size(max = 500, message = "Use no máximo 500 caracteres.") String titulo,
        @Size(max = 500, message = "Use no máximo 500 caracteres.") String subtitulo,
        // Itens em branco são descartados no service (linhas vazias do formulário)
        @Size(max = 20, message = "Informe no máximo 20 autores.")
        List<@Size(max = 255, message = "Use no máximo 255 caracteres.") String> autores,
        @Size(max = 255, message = "Use no máximo 255 caracteres.") String editora,
        @Min(value = 1400, message = "Ano inválido.") @Max(value = 2100, message = "Ano inválido.") Integer ano,
        @Positive(message = "Deve ser maior que zero.") @Max(value = 99999, message = "Número de páginas inválido.") Integer paginas,
        @Size(max = 10, message = "Use o código do idioma, ex.: pt.") String idioma,
        @Size(max = 20000, message = "Use no máximo 20.000 caracteres.") String sinopse,
        @Size(max = 30, message = "Informe no máximo 30 assuntos.")
        List<@Size(max = 150, message = "Use no máximo 150 caracteres.") String> assuntos,
        FonteIsbn fonteDados) {
}
