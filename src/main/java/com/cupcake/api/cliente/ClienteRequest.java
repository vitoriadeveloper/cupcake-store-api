package com.cupcake.api.cliente;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record ClienteRequest(
    @NotBlank
    @Size(max = 120)
    String nome,
    @NotBlank
    @Email
    @Size(max = 254)
    String email,
    @NotBlank
    @Size(max = 30)
    String telefone
) {
    public ClienteRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
