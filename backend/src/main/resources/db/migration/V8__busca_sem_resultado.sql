-- Buscas do site público que não acharam nenhum livro, contadas por termo e dia: mostram o que procuram
-- e o acervo não tem (ajuda a decidir compras e pedidos de doação). Só o termo e a data: nada de IP,
-- leitor ou sessão. A collation do banco junta maiúsculas e acentos no mesmo termo.
CREATE TABLE busca_sem_resultado (
    termo VARCHAR(100) NOT NULL,
    dia   DATE         NOT NULL,
    vezes INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (termo, dia),
    CONSTRAINT ck_busca_vezes CHECK (vezes > 0)
);

CREATE INDEX ix_busca_dia ON busca_sem_resultado (dia);
