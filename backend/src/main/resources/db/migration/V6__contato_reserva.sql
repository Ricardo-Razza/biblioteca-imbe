-- Contato informado pelo leitor ao reservar pelo site: a equipe usa para avisar que o livro está separado.
-- Fica na reserva e não altera o cadastro do leitor. Reservas antigas ficam sem contato próprio.
ALTER TABLE reserva
    ADD COLUMN telefone VARCHAR(20),
    ADD COLUMN email    VARCHAR(150);
