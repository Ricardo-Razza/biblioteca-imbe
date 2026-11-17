package br.gov.imbe.biblioteca.comum.auditoria;

/** O que fica registrado na auditoria, com o texto mostrado na tela de Configurações. */
public enum AcaoAuditoria {
    LOGIN("Entrou no sistema"),
    LOGIN_RECUSADO("Login recusado"),
    LOGIN_TRAVADO("Login travado por tentativas"),
    SENHA_TROCADA("Trocou a própria senha"),
    SENHA_REDEFINIDA("Redefiniu a senha de um funcionário"),
    FUNCIONARIO_CADASTRADO("Cadastrou funcionário"),
    FUNCIONARIO_ALTERADO("Alterou funcionário"),
    PARAMETRO_ALTERADO("Alterou parâmetro"),
    FERIADO_INCLUIDO("Incluiu feriado"),
    FERIADO_REMOVIDO("Removeu feriado"),
    FERIADOS_IMPORTADOS("Importou feriados nacionais"),
    LEITOR_DESBLOQUEADO("Desbloqueou leitor"),
    EXEMPLAR_ALTERADO("Alterou exemplar"),
    RESERVA_CANCELADA("Cancelou reserva");

    private final String descricao;

    AcaoAuditoria(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
