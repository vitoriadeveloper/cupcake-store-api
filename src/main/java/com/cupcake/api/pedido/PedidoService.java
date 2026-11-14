package com.cupcake.api.pedido;

import com.cupcake.api.cliente.ClienteRepository;
import com.cupcake.api.cupcake.CupcakeRepository;
import com.cupcake.api.shared.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {
    private final PedidoRepository repository;
    private final ClienteRepository clientes;
    private final CupcakeRepository cupcakes;

    public PedidoResponse criar(PedidoRequest r) {
        Pedido p = new Pedido();
        p.setCliente(clientes.findById(r.clienteId())
            .orElseThrow(() -> new NaoEncontradoException("Cliente nao encontrado.")));
        p.setEndereco(r.endereco().entidade());
        BigDecimal total = BigDecimal.ZERO;
        var ids = new HashSet<Long>();
        for (var item : r.itens()) {
            if (!ids.add(item.cupcakeId()))
                throw new RegraNegocioException("Cupcake repetido no pedido.");
            var c = cupcakes.findById(item.cupcakeId())
                .orElseThrow(() -> new NaoEncontradoException("Cupcake nao encontrado."));
            if (!c.isAtivo() || !c.isDisponivel())
                throw new RegraNegocioException("Cupcake indisponivel.");
            ItemPedido i = new ItemPedido();
            i.setPedido(p);
            i.setCupcake(c);
            i.setNomeCupcake(c.getNome());
            i.setQuantidade(item.quantidade());
            i.setPrecoUnitario(c.getPreco());
            p.getItens().add(i);
            total = total.add(c.getPreco().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        if (p.getItens().isEmpty())
            throw new RegraNegocioException("Pedido deve conter itens.");
        if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new RegraNegocioException("Valor do pedido excede o limite.");
        p.setValorTotal(total);
        return PedidoResponse.de(repository.save(p));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id) {
        return PedidoResponse.de(obter(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return repository.findAll().stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> historico(Long clienteId) {
        if (!clientes.existsById(clienteId))
            throw new NaoEncontradoException("Cliente nao encontrado.");
        return repository.findByClienteIdOrderByIdDesc(clienteId).stream().map(PedidoResponse::de).toList();
    }

    public PedidoResponse atualizarStatus(Long id, StatusPedido novo) {
        Pedido p = obter(id);
        boolean permitido = switch (p.getStatus()) {
            case PEDIDO_ENVIADO -> novo == StatusPedido.PREPARANDO;
            case PREPARANDO -> novo == StatusPedido.PRONTO;
            case PRONTO -> novo == StatusPedido.ENTREGUE;
            default -> false;
        };
        if (!permitido)
            throw new RegraNegocioException("Transicao de status nao permitida.");
        p.setStatus(novo);
        return PedidoResponse.de(p);
    }

    public PedidoResponse cancelar(Long id) {
        Pedido p = obter(id);
        if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO && p.getStatus() != StatusPedido.PEDIDO_ENVIADO)
            throw new RegraNegocioException("Pedido so pode ser cancelado antes da preparacao.");
        p.setStatus(StatusPedido.CANCELADO);
        if (p.getStatusPagamento() == StatusPagamento.APROVADO_SIMULADO)
            p.setStatusPagamento(StatusPagamento.CANCELADO_SIMULADO);
        return PedidoResponse.de(p);
    }

    private Pedido obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido nao encontrado."));
    }
}
