package br.gov.imbe.biblioteca.importacao;

/** O que aconteceu com uma linha da planilha. */
public enum ResultadoItem {
    /** Ainda não processada. */
    PENDENTE,
    /** Obra nova cadastrada com os dados da fonte (e o exemplar, se a linha tinha tombo). */
    CADASTRADO,
    /** A obra já existia: só o exemplar foi incluído. */
    EXEMPLAR_ADICIONADO,
    /** A obra já existia e a linha não tinha tombo: nada a fazer. */
    JA_CADASTRADO,
    /** Nenhuma fonte conhece o ISBN: cadastrar à mão. */
    NAO_ENCONTRADO,
    /** Alguma fonte falhou e as outras não acharam: dá para tentar de novo. */
    FONTE_INDISPONIVEL,
    /** Linha inválida ou conflito (ISBN inválido, tombo repetido...). */
    ERRO;

    boolean finalizado() {
        return this != PENDENTE;
    }
}
