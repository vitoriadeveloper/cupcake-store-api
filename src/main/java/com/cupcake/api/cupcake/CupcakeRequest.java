package com.cupcake.api.cupcake;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CupcakeRequest(
    @NotBlank
    @Size(max = 120)
    String nome,
    @NotBlank
    @Size(max = 1000)
    String descricao,
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal preco,
    @NotBlank
    @Size(max = 120)
    String sabor,
    @Size(max = 1000)
    String imagemUrl,
    @NotNull
    Boolean disponivel
) {
}
