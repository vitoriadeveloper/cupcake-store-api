-- Duplicidades existentes devem ser resolvidas antes de aplicar esta migration.
-- Nenhum cliente ou pedido e removido automaticamente.
UPDATE cupcake.tb_clientes SET email = LOWER(TRIM(email));

ALTER TABLE cupcake.tb_clientes
    ADD CONSTRAINT uk_clientes_email UNIQUE (email);

ALTER TABLE cupcake.tb_clientes
    ADD CONSTRAINT ck_clientes_email_normalizado CHECK (email = LOWER(TRIM(email)));
