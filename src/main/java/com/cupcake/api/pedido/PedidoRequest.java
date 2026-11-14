package com.cupcake.api.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PedidoRequest(
    @NotNull
    @Positive
    Long clienteId,
    @NotEmpty
    @Size(max = 100)
    List<@NotNull @Valid ItemRequest> itens,
    @NotNull
    @Valid
    EnderecoRequest endereco
) {
    public record ItemRequest(
        @NotNull
        @Positive
        Long cupcakeId,
        @NotNull
        @Min(1)
        @Max(100)
        Integer quantidade
    ) {
    }

    public record EnderecoRequest(
        @NotBlank
        @Size(max = 200)
        String logradouro,
        @NotBlank
        @Size(max = 30)
        String numero,
        @Size(max = 120)
        String complemento,
        @NotBlank
        @Size(max = 120)
        String bairro,
        @NotBlank
        @Size(max = 120)
        String cidade,
        @NotBlank
        @Pattern(regexp = "[A-Z]{2}")
        String estado,
        @NotBlank
        @Pattern(regexp = "[0-9]{5}-?[0-9]{3}")
        String cep
    ) {
        public Endereco entidade() {
            Endereco e = new Endereco();
            e.setLogradouro(logradouro);
            e.setNumero(numero);
            e.setComplemento(complemento);
            e.setBairro(bairro);
            e.setCidade(cidade);
            e.setEstado(estado);
            e.setCep(cep);
            return e;
        }

        public static EnderecoRequest de(Endereco e) {
            return new EnderecoRequest(e.getLogradouro(), e.getNumero(), e.getComplemento(),
                e.getBairro(), e.getCidade(), e.getEstado(), e.getCep());
        }
    }
}
