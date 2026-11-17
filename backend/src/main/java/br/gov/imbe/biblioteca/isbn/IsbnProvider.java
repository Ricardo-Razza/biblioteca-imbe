package br.gov.imbe.biblioteca.isbn;

import java.util.Optional;

/**
 * Uma fonte externa de dados por ISBN (BrasilAPI, Google Books, Open Library).
 * As implementações são orquestradas pelo {@code IsbnLookupService}.
 */
public interface IsbnProvider {

    FonteIsbn fonte();

    /**
     * @return os dados, ou vazio se a fonte respondeu que não conhece o ISBN
     * @throws FonteIsbnIndisponivelException se a fonte falhou (timeout, erro HTTP, resposta ilegível).
     *         Diferente de "não encontrado": esse resultado não pode ir para o cache.
     */
    Optional<DadosLivro> buscar(Isbn isbn);
}
