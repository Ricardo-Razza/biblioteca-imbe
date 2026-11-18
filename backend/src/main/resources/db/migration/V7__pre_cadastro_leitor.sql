-- Leitor criado pelo site ao reservar: os dados vêm do próprio cidadão e a equipe confere com o documento
-- na retirada. Enquanto pendente, reserva normalmente, mas não leva livro.
ALTER TABLE leitor
    ADD COLUMN cadastro_pendente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_leitor_pendente ON leitor (cadastro_pendente);
