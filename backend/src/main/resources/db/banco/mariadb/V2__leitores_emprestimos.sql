-- Fase 2: parâmetros, leitores e empréstimos.
-- Os campos de funcionário do empréstimo ficam sem FK até a tabela funcionario existir (fase 3).

CREATE TABLE parametro (
    chave     VARCHAR(50)  NOT NULL,
    valor     VARCHAR(255) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    CONSTRAINT pk_parametro PRIMARY KEY (chave)
);

INSERT INTO parametro (chave, valor, descricao) VALUES
    ('PRAZO_EMPRESTIMO_DIAS', '14', 'Dias corridos de cada empréstimo'),
    ('LIMITE_LIVROS_POR_LEITOR', '3', 'Livros ao mesmo tempo por leitor (empréstimos + reservas)'),
    ('MAXIMO_RENOVACOES', '2', 'Renovações por empréstimo'),
    ('DIAS_POR_RENOVACAO', '14', 'Dias corridos de cada renovação, contados do dia da renovação'),
    ('PRAZO_RETIRADA_RESERVA_DIAS_UTEIS', '2', 'Dias úteis para retirar uma reserva');

CREATE TABLE leitor (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(200) NOT NULL,
    cpf             CHAR(11)     NOT NULL,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20),
    email           VARCHAR(150),
    endereco        VARCHAR(255),
    responsavel     VARCHAR(200),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    bloqueado_ate   DATE,
    criado_em       DATETIME     NOT NULL,
    CONSTRAINT pk_leitor PRIMARY KEY (id),
    CONSTRAINT uk_leitor_cpf UNIQUE (cpf),
    CONSTRAINT ck_leitor_cpf CHECK (cpf REGEXP '^[0-9]{11}$')
);

CREATE INDEX ix_leitor_nome ON leitor (nome);

CREATE TABLE emprestimo (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    exemplar_id              BIGINT       NOT NULL,
    leitor_id                BIGINT       NOT NULL,
    funcionario_saida_id     BIGINT,
    data_saida               DATE         NOT NULL,
    data_prevista            DATE         NOT NULL,
    data_devolucao           DATE,
    funcionario_devolucao_id BIGINT,
    renovacoes               SMALLINT     NOT NULL DEFAULT 0,
    ocorrencia_devolucao     VARCHAR(20),
    observacao               VARCHAR(500),
    -- Um único empréstimo em aberto por exemplar, garantido pelo banco (MySQL não tem índice parcial)
    exemplar_em_aberto       BIGINT GENERATED ALWAYS AS (CASE WHEN data_devolucao IS NULL THEN exemplar_id END) STORED,
    CONSTRAINT pk_emprestimo PRIMARY KEY (id),
    CONSTRAINT fk_emprestimo_exemplar FOREIGN KEY (exemplar_id) REFERENCES exemplar (id),
    CONSTRAINT fk_emprestimo_leitor FOREIGN KEY (leitor_id) REFERENCES leitor (id),
    CONSTRAINT uk_emprestimo_exemplar_em_aberto UNIQUE (exemplar_em_aberto),
    CONSTRAINT ck_emprestimo_ocorrencia CHECK (ocorrencia_devolucao IN ('DANIFICADO', 'EXTRAVIADO')),
    CONSTRAINT ck_emprestimo_datas CHECK (data_prevista >= data_saida)
);

CREATE INDEX ix_emprestimo_leitor ON emprestimo (leitor_id, data_devolucao);
CREATE INDEX ix_emprestimo_aberto ON emprestimo (data_devolucao, data_prevista);

-- Dias sem atendimento além de sábado e domingo. Nacionais vêm da BrasilAPI; municipais, do administrador.
CREATE TABLE feriado (
    data      DATE         NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    tipo      VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_feriado PRIMARY KEY (data),
    CONSTRAINT ck_feriado_tipo CHECK (tipo IN ('NACIONAL', 'MUNICIPAL'))
);
