package com.cupcake.api.pedido;

import com.cupcake.api.cupcake.Cupcake;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "tb_itens_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cupcake_id")
    private Cupcake cupcake;

    @Column(nullable = false, length = 120)
    private String nomeCupcake;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;
}

