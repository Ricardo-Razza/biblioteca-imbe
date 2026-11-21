-- Acervo: obra (dados do ISBN), autores, assuntos e exemplares (cópias físicas com tombo).
-- Enums como VARCHAR + CHECK, mapeados com @Enumerated(EnumType.STRING).
-- Sem ON DELETE CASCADE: nada com histórico é apagado.

CREATE TABLE obra (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    isbn13         CHAR(13)      NOT NULL,
    titulo         VARCHAR(500)  NOT NULL,
    subtitulo      VARCHAR(500)  NULL,
    editora        VARCHAR(255)  NULL,
    ano            SMALLINT      NULL,
    paginas        INT           NULL,
    idioma         VARCHAR(10)   NULL,
    sinopse        TEXT          NULL,
    capa_arquivo   VARCHAR(255)  NULL,
    capa_miniatura VARCHAR(255)  NULL,
    fonte_dados    VARCHAR(30)   NULL,
    criado_em      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_obra PRIMARY KEY (id),
    CONSTRAINT uk_obra_isbn13 UNIQUE (isbn13),
    CONSTRAINT ck_obra_isbn13 CHECK (isbn13 REGEXP '^97[89][0-9]{10}$'),
    CONSTRAINT ck_obra_paginas CHECK (paginas IS NULL OR paginas > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE INDEX ix_obra_titulo ON obra (titulo);

CREATE TABLE autor (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    CONSTRAINT pk_autor PRIMARY KEY (id),
    CONSTRAINT uk_autor_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE obra_autor (
    obra_id  BIGINT   NOT NULL,
    autor_id BIGINT   NOT NULL,
    ordem    SMALLINT NOT NULL,
    CONSTRAINT pk_obra_autor PRIMARY KEY (obra_id, autor_id),
    CONSTRAINT fk_obra_autor_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT fk_obra_autor_autor FOREIGN KEY (autor_id) REFERENCES autor (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE INDEX ix_obra_autor_autor ON obra_autor (autor_id);

CREATE TABLE assunto (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    CONSTRAINT pk_assunto PRIMARY KEY (id),
    CONSTRAINT uk_assunto_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE obra_assunto (
    obra_id    BIGINT NOT NULL,
    assunto_id BIGINT NOT NULL,
    CONSTRAINT pk_obra_assunto PRIMARY KEY (obra_id, assunto_id),
    CONSTRAINT fk_obra_assunto_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT fk_obra_assunto_assunto FOREIGN KEY (assunto_id) REFERENCES assunto (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

CREATE INDEX ix_obra_assunto_assunto ON obra_assunto (assunto_id);

CREATE TABLE exemplar (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    obra_id        BIGINT       NOT NULL,
    tombo          VARCHAR(30)  NOT NULL,
    localizacao    VARCHAR(100) NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'BOM',
    situacao       VARCHAR(20)  NOT NULL DEFAULT 'DISPONIVEL',
    data_aquisicao DATE         NULL,
    origem         VARCHAR(20)  NULL,
    criado_em      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_exemplar PRIMARY KEY (id),
    CONSTRAINT uk_exemplar_tombo UNIQUE (tombo),
    CONSTRAINT fk_exemplar_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT ck_exemplar_estado CHECK (estado IN ('BOM', 'DANIFICADO')),
    CONSTRAINT ck_exemplar_situacao CHECK (situacao IN ('DISPONIVEL', 'RESERVADO', 'EMPRESTADO', 'EXTRAVIADO', 'BAIXADO')),
    CONSTRAINT ck_exemplar_origem CHECK (origem IS NULL OR origem IN ('COMPRA', 'DOACAO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci;

-- Conta os disponíveis por obra e serve de índice para a FK obra_id
CREATE INDEX ix_exemplar_obra_situacao ON exemplar (obra_id, situacao);
