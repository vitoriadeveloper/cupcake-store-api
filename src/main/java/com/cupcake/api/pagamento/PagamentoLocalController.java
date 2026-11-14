package com.cupcake.api.pagamento;

import com.cupcake.api.pedido.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/pedidos/{id}/pagamento-simulado")
public class PagamentoLocalController {
    private final PagamentoLocalService service;

    public record PagamentoRequest(
        @NotNull
        FormaPagamento formaPagamento
    ) {
    }

    @PostMapping
    public PedidoResponse pagar(
        @PathVariable
        Long id,
        @Valid
        @RequestBody
        PagamentoRequest r
    ) {
        return service.aprovar(id, r.formaPagamento());
    }
}
