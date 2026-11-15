package com.cupcake.api.pagamento;

import com.cupcake.api.pedido.*;
import com.cupcake.api.shared.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
@RequiredArgsConstructor
@Log4j2
public class PagamentoLocalService {
    private final PedidoRepository pedidos;

    @Transactional
    public PedidoResponse aprovar(Long id, FormaPagamento forma) {
        if (forma == null) {
            log.warn("evento=pagamento_rejeitado pedidoId={} motivo=forma_ausente", id);
            throw new RegraNegocioException("Escolha CREDITO ou DEBITO.");
        }
        Pedido p = pedidos.findById(id).orElseThrow(() -> {
            log.warn("evento=pagamento_rejeitado pedidoId={} motivo=pedido_nao_encontrado", id);
            return new NaoEncontradoException("Pedido nao encontrado.");
        });
        if (p.getStatusPagamento() == StatusPagamento.APROVADO_SIMULADO
                && p.getStatus() != StatusPedido.CANCELADO) {
            if (p.getFormaPagamento() != forma) {
                log.warn("evento=pagamento_rejeitado pedidoId={} motivo=forma_diferente forma={}", id, forma);
                throw new RegraNegocioException("Pedido ja pago com outra forma de pagamento.");
            }
            log.info("evento=pagamento_repetido pedidoId={} forma={} resultado=aprovacao_existente", id, forma);
            return PedidoResponse.de(p);
        }
        if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            log.warn("evento=pagamento_rejeitado pedidoId={} motivo=status_incompativel statusPedido={}", id, p.getStatus());
            throw new RegraNegocioException("Pedido nao permite pagamento.");
        }
        p.setFormaPagamento(forma);
        p.setStatusPagamento(StatusPagamento.APROVADO_SIMULADO);
        p.setStatus(StatusPedido.PEDIDO_ENVIADO);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("evento=pagamento_aprovado_simulado pedidoId={} forma={} statusPedido=PEDIDO_ENVIADO", id, forma);
                }
            });
        return PedidoResponse.de(p);
    }
}
