package com.cupcake.api.cupcake;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cupcakes")
@RequiredArgsConstructor
public class CupcakeController {
    private final CupcakeService service;

    @GetMapping
    public List<CupcakeResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CupcakeResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<CupcakeResponse> criar(@Valid @RequestBody CupcakeRequest r) {
        var resultado = service.criar(r);
        return ResponseEntity.ok().body(resultado);
    }

    @PutMapping("/{id}")
    public CupcakeResponse atualizar(
        @PathVariable
        Long id,
        @Valid
        @RequestBody
        CupcakeRequest r
    ) {
        return service.atualizar(id, r);
    }

    public record DisponibilidadeRequest(
        @NotNull
        Boolean disponivel
    ) {
    }

    @PatchMapping("/{id}/disponibilidade")
    public CupcakeResponse disponibilidade(
        @PathVariable
        Long id,
        @Valid
        @RequestBody
        DisponibilidadeRequest r
    ) {
        return service.disponibilidade(id, r.disponivel());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
