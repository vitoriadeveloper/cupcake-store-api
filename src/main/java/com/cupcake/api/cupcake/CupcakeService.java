package com.cupcake.api.cupcake;

import com.cupcake.api.shared.NaoEncontradoException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CupcakeService {
    private final CupcakeRepository repository;

    @Transactional(readOnly = true)
    public List<CupcakeResponse> listar() {
        return repository.findByAtivoTrueOrderByIdAsc().stream().map(CupcakeResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CupcakeResponse buscar(Long id) {
        return CupcakeResponse.de(obter(id));
    }

    public CupcakeResponse criar(CupcakeRequest r) {
        Cupcake c = new Cupcake();
        preencher(c, r);
        return CupcakeResponse.de(repository.save(c));
    }

    public CupcakeResponse atualizar(Long id, CupcakeRequest r) {
        Cupcake c = obter(id);
        preencher(c, r);
        return CupcakeResponse.de(c);
    }

    public CupcakeResponse disponibilidade(Long id, boolean disponivel) {
        Cupcake c = obter(id);
        c.setDisponivel(disponivel);
        return CupcakeResponse.de(c);
    }

    public void inativar(Long id) {
        Cupcake c = obter(id);
        c.setAtivo(false);
        c.setDisponivel(false);
    }

    private Cupcake obter(Long id) {
        return repository.findById(id).filter(Cupcake::isAtivo)
            .orElseThrow(() -> new NaoEncontradoException("Cupcake nao encontrado."));
    }

    private void preencher(Cupcake c, CupcakeRequest r) {
        c.setNome(r.nome());
        c.setDescricao(r.descricao());
        c.setPreco(r.preco());
        c.setSabor(r.sabor());
        c.setImagemUrl(r.imagemUrl());
        c.setDisponivel(r.disponivel());
    }
}
