package com.cupcake.api.pedido;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Endereco {
    @Column(nullable = false, length = 200)
    private String logradouro;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(length = 120)
    private String complemento;

    @Column(nullable = false, length = 120)
    private String bairro;

    @Column(nullable = false, length = 120)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false, length = 9)
    private String cep;
}

