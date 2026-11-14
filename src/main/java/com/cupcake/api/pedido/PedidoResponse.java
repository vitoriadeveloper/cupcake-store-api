package com.cupcake.api.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
    Long id,
    Long clienteId,
    List<ItemResponse> itens,
    BigDecimal valorTotal,
    StatusPedido status,
    StatusPagamento statusPagamento,
    FormaPagamento formaPagamento,
    Instant dataCriacao,
    PedidoRequest.EnderecoRequest endereco
) {
    public record ItemResponse(
        Long cupcakeId,
        String nome,
        int quantidade,
        BigDecimal precoUnitario
    ) {
    }

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(p.getId(), p.getCliente().getId(),
            p.getItens().stream().map(i -> new ItemResponse(i.getCupcake().getId(),
                i.getNomeCupcake(), i.getQuantidade(), i.getPrecoUnitario())).toList(),
            p.getValorTotal(), p.getStatus(), p.getStatusPagamento(), p.getFormaPagamento(),
            p.getDataCriacao(), PedidoRequest.EnderecoRequest.de(p.getEndereco()));
    }
}
