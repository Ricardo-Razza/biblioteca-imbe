-- Fase 4: reservas pelo site público.
-- A reserva prende um exemplar (situação RESERVADO) até a retirada, o cancelamento ou o fim do prazo.

CREATE TABLE reserva (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    protocolo       CHAR(8)     NOT NULL,
    obra_id         BIGINT      NOT NULL,
    exemplar_id     BIGINT      NOT NULL,
    leitor_id       BIGINT      NOT NULL,
    criada_em       DATETIME    NOT NULL,
    prazo_retirada  DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    emprestimo_id   BIGINT,
    finalizada_em   DATETIME,
    cancelada_por   VARCHAR(20),
    funcionario_id  BIGINT,
    -- Uma reserva aguardando por exemplar e uma por leitor e obra, garantidas pelo banco
    -- (MySQL não tem índice parcial; NULL não conflita no índice único)
    exemplar_aguardando BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'AGUARDANDO_RETIRADA' THEN exemplar_id END) STORED,
    leitor_aguardando   BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'AGUARDANDO_RETIRADA' THEN leitor_id END) STORED,
    CONSTRAINT pk_reserva PRIMARY KEY (id),
    CONSTRAINT uk_reserva_protocolo UNIQUE (protocolo),
    CONSTRAINT uk_reserva_exemplar_aguardando UNIQUE (exemplar_aguardando),
    CONSTRAINT uk_reserva_leitor_obra_aguardando UNIQUE (leitor_aguardando, obra_id),
    CONSTRAINT fk_reserva_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT fk_reserva_exemplar FOREIGN KEY (exemplar_id) REFERENCES exemplar (id),
    CONSTRAINT fk_reserva_leitor FOREIGN KEY (leitor_id) REFERENCES leitor (id),
    CONSTRAINT fk_reserva_emprestimo FOREIGN KEY (emprestimo_id) REFERENCES emprestimo (id),
    CONSTRAINT fk_reserva_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario (id),
    CONSTRAINT ck_reserva_status CHECK (status IN ('AGUARDANDO_RETIRADA', 'RETIRADA', 'CANCELADA', 'EXPIRADA')),
    CONSTRAINT ck_reserva_cancelada_por CHECK (cancelada_por IN ('LEITOR', 'BIBLIOTECA')),
    CONSTRAINT ck_reserva_retirada CHECK ((status = 'RETIRADA') = (emprestimo_id IS NOT NULL)),
    CONSTRAINT ck_reserva_finalizada CHECK ((status = 'AGUARDANDO_RETIRADA') = (finalizada_em IS NULL))
);

CREATE INDEX ix_reserva_status ON reserva (status, prazo_retirada);
CREATE INDEX ix_reserva_leitor ON reserva (leitor_id, criada_em);
