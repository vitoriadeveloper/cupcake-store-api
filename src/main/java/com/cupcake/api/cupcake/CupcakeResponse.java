package com.cupcake.api.cupcake;

import java.math.BigDecimal;

public record CupcakeResponse(
    Long id,
    String nome,
    String descricao,
    BigDecimal preco,
    String sabor,
    String imagemUrl,
    boolean disponivel
) {
    public static CupcakeResponse de(Cupcake c) {
        return new CupcakeResponse(c.getId(), c.getNome(), c.getDescricao(), c.getPreco(),
            c.getSabor(), c.getImagemUrl(), c.isDisponivel());
    }
}
