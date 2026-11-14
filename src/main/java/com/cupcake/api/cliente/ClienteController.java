package com.cupcake.api.cliente;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService service;

    @GetMapping
    public List<ClienteResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest r) {
        var resultado = service.criar(r);
        return ResponseEntity.ok().body(resultado);
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(
        @PathVariable
        Long id,
        @Valid
        @RequestBody
        ClienteRequest r
    ) {
        return service.atualizar(id, r);
    }
}
