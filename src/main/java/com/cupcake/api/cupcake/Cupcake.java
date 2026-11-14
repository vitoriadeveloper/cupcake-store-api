package com.cupcake.api.cupcake;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "tb_cupcakes")
@Getter
@Setter
@NoArgsConstructor
public class Cupcake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false, length = 120)
    private String sabor;

    @Column(length = 1000)
    private String imagemUrl;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(nullable = false)
    private boolean ativo = true;
}

