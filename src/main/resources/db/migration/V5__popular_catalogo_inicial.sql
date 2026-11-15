-- As imagens sao recursos estaticos do JAR; o banco armazena apenas a URL.
-- Preserva produtos existentes com o mesmo nome, inclusive os inativados.
INSERT INTO cupcake.tb_cupcakes (nome, descricao, preco, sabor, imagem_url, disponivel, ativo)
SELECT catalogo.nome, catalogo.descricao, catalogo.preco, catalogo.sabor, catalogo.imagem_url, TRUE, TRUE
FROM (VALUES
    ('Cupcake de Chocolate', 'Massa de chocolate com cobertura de ganache.', 12.50, 'Chocolate', '/images/cupcakes/chocolate.png'),
    ('Cupcake de Morango', 'Massa fofinha com cobertura de morango.', 12.50, 'Morango', '/images/cupcakes/morango.png'),
    ('Cupcake de Baunilha', 'Massa de baunilha com cobertura cremosa.', 11.00, 'Baunilha', '/images/cupcakes/baunilha.png'),
    ('Cupcake de Limão', 'Massa de limão com cobertura cítrica.', 12.00, 'Limão', '/images/cupcakes/limao.png'),
    ('Cupcake Red Velvet', 'Massa red velvet com cobertura de cream cheese.', 14.00, 'Red Velvet', '/images/cupcakes/red-velvet.png'),
    ('Cupcake de Cenoura', 'Massa de cenoura com cobertura de chocolate.', 12.00, 'Cenoura', '/images/cupcakes/cenoura.png'),
    ('Cupcake de Coco', 'Massa de coco com cobertura e coco ralado.', 12.00, 'Coco', '/images/cupcakes/coco.png'),
    ('Cupcake de Doce de Leite', 'Massa fofinha com cobertura de doce de leite.', 13.50, 'Doce de Leite', '/images/cupcakes/doce-de-leite.png')
) AS catalogo(nome, descricao, preco, sabor, imagem_url)
WHERE NOT EXISTS (
    SELECT 1 FROM cupcake.tb_cupcakes existente
    WHERE LOWER(TRIM(existente.nome)) = LOWER(catalogo.nome)
);
