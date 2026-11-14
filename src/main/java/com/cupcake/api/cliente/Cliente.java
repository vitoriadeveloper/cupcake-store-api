package com.cupcake.api.cliente;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String telefone;
}

