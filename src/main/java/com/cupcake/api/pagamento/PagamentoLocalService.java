package com.cupcake.api.pagamento;

import com.cupcake.api.pedido.*;
import com.cupcake.api.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
@RequiredArgsConstructor
public class PagamentoLocalService {
    private final PedidoRepository pedidos;

    @Transactional
    public PedidoResponse aprovar(Long id, FormaPagamento forma) {
        if (forma == null)
            throw new RegraNegocioException("Escolha CREDITO ou DEBITO.");
        Pedido p = pedidos.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido nao encontrado."));
        if (p.getStatusPagamento() == StatusPagamento.APROVADO_SIMULADO
                && p.getStatus() != StatusPedido.CANCELADO) {
            if (p.getFormaPagamento() != forma)
                throw new RegraNegocioException("Pedido ja pago com outra forma de pagamento.");
            return PedidoResponse.de(p);
        }
        if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO)
            throw new RegraNegocioException("Pedido nao permite pagamento.");
        p.setFormaPagamento(forma);
        p.setStatusPagamento(StatusPagamento.APROVADO_SIMULADO);
        p.setStatus(StatusPedido.PEDIDO_ENVIADO);
        return PedidoResponse.de(p);
    }
}
