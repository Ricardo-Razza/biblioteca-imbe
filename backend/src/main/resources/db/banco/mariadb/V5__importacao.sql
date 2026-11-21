-- Fase 6: carga inicial do acervo por planilha de ISBNs.
-- Cada linha da planilha vira um item; o processamento busca o ISBN nas fontes e cadastra obra e exemplar.

CREATE TABLE importacao (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    arquivo        VARCHAR(255) NOT NULL,
    funcionario_id BIGINT,
    criada_em      DATETIME     NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    finalizada_em  DATETIME,
    CONSTRAINT pk_importacao PRIMARY KEY (id),
    CONSTRAINT fk_importacao_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario (id),
    CONSTRAINT ck_importacao_status CHECK (status IN ('PROCESSANDO', 'CONCLUIDA')),
    CONSTRAINT ck_importacao_finalizada CHECK ((status = 'CONCLUIDA') = (finalizada_em IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE importacao_item (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    importacao_id BIGINT       NOT NULL,
    linha         INT          NOT NULL,
    isbn_original VARCHAR(40)  NOT NULL,
    isbn13        CHAR(13),
    tombo         VARCHAR(30),
    localizacao   VARCHAR(100),
    resultado     VARCHAR(20)  NOT NULL,
    obra_id       BIGINT,
    mensagem      VARCHAR(500),
    CONSTRAINT pk_importacao_item PRIMARY KEY (id),
    CONSTRAINT fk_importacao_item_importacao FOREIGN KEY (importacao_id) REFERENCES importacao (id),
    CONSTRAINT fk_importacao_item_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT ck_importacao_item_resultado CHECK (resultado IN
        ('PENDENTE', 'CADASTRADO', 'EXEMPLAR_ADICIONADO', 'JA_CADASTRADO', 'NAO_ENCONTRADO', 'FONTE_INDISPONIVEL', 'ERRO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE INDEX ix_importacao_item_resultado ON importacao_item (importacao_id, resultado, linha);
