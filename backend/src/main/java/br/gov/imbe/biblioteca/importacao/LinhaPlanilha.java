package br.gov.imbe.biblioteca.importacao;

/**
 * Uma linha já lida da planilha.
 *
 * @param numero       número da linha no arquivo (a partir de 1), para o funcionário achar
 * @param isbnOriginal como estava na planilha
 * @param isbn13       normalizado; nulo quando inválido
 * @param tombo        normalizado (maiúsculas); nulo quando a linha não traz tombo
 * @param erro         motivo para não processar a linha; nulo quando ela está boa
 */
record LinhaPlanilha(int numero, String isbnOriginal, String isbn13, String tombo, String localizacao, String erro) {
}
