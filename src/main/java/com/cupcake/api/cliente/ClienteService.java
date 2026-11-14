package com.cupcake.api.cliente;

import com.cupcake.api.shared.NaoEncontradoException;
import com.cupcake.api.shared.RegraNegocioException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {
    private final ClienteRepository repository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(ClienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(obter(id));
    }

    public ClienteResponse criar(ClienteRequest r) {
        if (repository.existsByEmail(r.email())) {
            throw new RegraNegocioException("Ja existe um cliente cadastrado com este e-mail.");
        }
        Cliente c = new Cliente();
        preencher(c, r);
        return ClienteResponse.de(repository.save(c));
    }

    public ClienteResponse atualizar(Long id, ClienteRequest r) {
        Cliente c = obter(id);
        if (repository.existsByEmailAndIdNot(r.email(), id)) {
            throw new RegraNegocioException("Ja existe um cliente cadastrado com este e-mail.");
        }
        preencher(c, r);
        return ClienteResponse.de(c);
    }

    private Cliente obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente nao encontrado."));
    }

    private void preencher(Cliente c, ClienteRequest r) {
        c.setNome(r.nome());
        c.setEmail(r.email());
        c.setTelefone(r.telefone());
    }
}
