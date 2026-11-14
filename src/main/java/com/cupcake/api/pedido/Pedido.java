package com.cupcake.api.pedido;

import com.cupcake.api.cliente.Cliente;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "tb_pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<ItemPedido> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Enumerated(EnumType.STRING) @Column(length = 30)
    private FormaPagamento formaPagamento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private Instant dataCriacao = Instant.now();

    @Embedded
    private Endereco endereco;

    @Version
    private Long versao;
}

