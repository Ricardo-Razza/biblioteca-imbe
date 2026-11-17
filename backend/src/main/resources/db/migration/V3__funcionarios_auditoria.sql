-- Fase 3: funcionários (login AD ou local), auditoria e quem registrou cada empréstimo.

CREATE TABLE funcionario (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    nome              VARCHAR(150) NOT NULL,
    login             VARCHAR(60)  NOT NULL,
    tipo_autenticacao VARCHAR(10)  NOT NULL,
    -- BCrypt; só para LOCAL. Usuário AD valida a senha na API da Prefeitura.
    senha_hash        VARCHAR(100),
    perfil            VARCHAR(12)  NOT NULL,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Senha provisória (cadastro ou redefinição): obriga a troca no próximo login
    trocar_senha      BOOLEAN      NOT NULL DEFAULT FALSE,
    tentativas_falhas SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_ate     DATETIME,
    ultimo_acesso     DATETIME,
    criado_em         DATETIME     NOT NULL,
    CONSTRAINT pk_funcionario PRIMARY KEY (id),
    CONSTRAINT uk_funcionario_login UNIQUE (login),
    CONSTRAINT ck_funcionario_tipo CHECK (tipo_autenticacao IN ('AD', 'LOCAL')),
    CONSTRAINT ck_funcionario_perfil CHECK (perfil IN ('ADMIN', 'ATENDENTE')),
    CONSTRAINT ck_funcionario_senha CHECK ((tipo_autenticacao = 'LOCAL') = (senha_hash IS NOT NULL))
);

ALTER TABLE emprestimo
    ADD CONSTRAINT fk_emprestimo_funcionario_saida FOREIGN KEY (funcionario_saida_id) REFERENCES funcionario (id),
    ADD CONSTRAINT fk_emprestimo_funcionario_devolucao FOREIGN KEY (funcionario_devolucao_id) REFERENCES funcionario (id);

-- Ações sensíveis: login, cadastro de funcionário, parâmetros, baixa de exemplar, desbloqueio etc.
CREATE TABLE auditoria (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    funcionario_id BIGINT,
    acao           VARCHAR(40) NOT NULL,
    entidade       VARCHAR(30),
    entidade_id    BIGINT,
    data_hora      DATETIME    NOT NULL,
    detalhe        TEXT,
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario (id)
);

CREATE INDEX ix_auditoria_data ON auditoria (data_hora);
CREATE INDEX ix_auditoria_entidade ON auditoria (entidade, entidade_id);
