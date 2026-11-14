package com.cupcake.api.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService service;

    @PostMapping("/api/pedidos")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest r) {
        var resultado = service.criar(r);
        return ResponseEntity.ok().body(resultado);
    }

    @GetMapping("/api/pedidos")
    public List<PedidoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/api/pedidos/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/api/clientes/{id}/pedidos")
    public List<PedidoResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    public record StatusRequest(
        @NotNull
        StatusPedido status
    ) {
    }

    @PatchMapping("/api/pedidos/{id}/status")
    public PedidoResponse status(
        @PathVariable
        Long id,
        @Valid
        @RequestBody
        StatusRequest r
    ) {
        return service.atualizarStatus(id, r.status());
    }

    @PatchMapping("/api/pedidos/{id}/cancelamento")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
