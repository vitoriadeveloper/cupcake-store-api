package com.cupcake.api;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
class CompraLocalIntegrationTests {
    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;
    final HttpClient http = HttpClient.newHttpClient();
    long cliente;
    long cupcake;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.update("DELETE FROM cupcake.tb_itens_pedido");
        jdbc.update("DELETE FROM cupcake.tb_pedidos");
        jdbc.update("DELETE FROM cupcake.tb_clientes");
        jdbc.update("DELETE FROM cupcake.tb_cupcakes");
        cliente = id(enviar("POST", "/api/clientes",
            """ 
            {"nome":"Ana","email":"ana@example.com","telefone":"71999999999"}
            """, 200));
        cupcake = id(enviar("POST", "/api/cupcakes",
            """
            {"nome":"Chocolate","descricao":"Cupcake gourmet","preco":12.50,"sabor":"Chocolate","disponivel":true}
            """, 201));
    }

    @Test
    void compraCreditoPreservaHistoricoEConcluiEntrega() throws Exception {
        long pedido = criarPedido();
        enviar("PATCH", "/api/pedidos/" + pedido + "/status", """ 
            {"status":"PREPARANDO"}
            """, 409);
        String pago = pagar(pedido, "CREDITO", 200);
        assertThat((String) JsonPath.read(pago, "$.status")).isEqualTo("PEDIDO_ENVIADO");
        assertThat((String) JsonPath.read(pago, "$.statusPagamento")).isEqualTo("APROVADO_SIMULADO");
        assertThat((String) JsonPath.read(pago, "$.formaPagamento")).isEqualTo("CREDITO");
        assertThat(((Number) JsonPath.read(pago, "$.valorTotal")).doubleValue()).isEqualTo(25.0);
        pagar(pedido, "CREDITO", 200);
        pagar(pedido, "DEBITO", 409);
        enviar("PUT", "/api/cupcakes/" + cupcake,
            """
            {"nome":"Chocolate novo","descricao":"Novo","preco":99.00,"sabor":"Chocolate","disponivel":true}
            """, 200);
        enviar("DELETE", "/api/cupcakes/" + cupcake, null, 204);
        String historico = enviar("GET", "/api/pedidos/" + pedido, null, 200);
        assertThat((String) JsonPath.read(historico, "$.itens[0].nome")).isEqualTo("Chocolate");
        assertThat(((Number) JsonPath.read(historico, "$.itens[0].precoUnitario")).doubleValue()).isEqualTo(12.5);
        assertThat((java.util.List<?>) JsonPath.read(
            enviar("GET", "/api/clientes/" + cliente + "/pedidos", null, 200), "$")).hasSize(1);
        for (String status : new String[]{"PREPARANDO", "PRONTO", "ENTREGUE"}) {
            enviar("PATCH", "/api/pedidos/" + pedido + "/status", "{\"status\":\"" + status + "\"}", 200);
        }
        enviar("PATCH", "/api/pedidos/" + pedido + "/cancelamento", null, 409);
    }

    @Test
    void debitoECancelamentoNaoPermitemReativarPedido() throws Exception {
        long pedido = criarPedido();
        pagar(pedido, "DEBITO", 200);
        String cancelado = enviar("PATCH", "/api/pedidos/" + pedido + "/cancelamento", null, 200);
        assertThat((String) JsonPath.read(cancelado, "$.statusPagamento")).isEqualTo("CANCELADO_SIMULADO");
        pagar(pedido, "DEBITO", 409);
        enviar("PATCH", "/api/pedidos/" + pedido + "/status", "{\"status\":\"PREPARANDO\"}", 409);
    }

    @Test
    void rejeitaPixDadosDeCartaoEFormaAusente() throws Exception {
        long pedido = criarPedido();
        pagar(pedido, "PIX", 400);
        enviar("POST", "/api/pedidos/" + pedido + "/pagamento-simulado", "{}", 400);
        enviar("POST", "/api/pedidos/" + pedido + "/pagamento-simulado",
            "{\"formaPagamento\":\"CREDITO\",\"numeroCartao\":\"1234\"}", 400);
        assertThat((String) JsonPath.read(enviar("GET", "/api/pedidos/" + pedido, null, 200),
            "$.statusPagamento")).isEqualTo("PENDENTE");
    }

    @Test
    void validaValoresItensDisponibilidadeERecursosAusentes() throws Exception {
        enviar("POST", "/api/cupcakes",
            """
            {"nome":"Chocolate","descricao":"Teste","preco":0,"sabor":"Chocolate","disponivel":true}
            """, 400);
        enviar("POST", "/api/clientes", "{\"nome\":\"Ana\",\"email\":\"invalido\",\"telefone\":\"71\"}", 400);
        enviar("POST", "/api/pedidos", pedidoJson("[]"), 400);
        enviar("POST", "/api/pedidos", pedidoJson("[{\"cupcakeId\":" + cupcake + ",\"quantidade\":0}]"), 400);
        enviar("POST", "/api/pedidos", pedidoJson("[{\"cupcakeId\":999999,\"quantidade\":1}]"), 404);
        enviar("POST", "/api/pedidos", pedidoJson("[{\"cupcakeId\":" + cupcake + ",\"quantidade\":1}]")
            .replace("\"clienteId\":" + cliente, "\"clienteId\":999999"), 404);
        enviar("PATCH", "/api/cupcakes/" + cupcake + "/disponibilidade", "{\"disponivel\":false}", 200);
        enviar("POST", "/api/pedidos", pedidoJson("[{\"cupcakeId\":" + cupcake + ",\"quantidade\":1}]"), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cupcake.tb_pedidos", Integer.class)).isZero();
    }

    @Test
    void naoAceitaTotalDoClienteNemItensRepetidos() throws Exception {
        String item = "{\"cupcakeId\":" + cupcake + ",\"quantidade\":1}";
        enviar("POST", "/api/pedidos", pedidoJson("[" + item + "," + item + "]"), 409);
        enviar("POST", "/api/pedidos", pedidoJson("[" + item + "]")
            .replace("\"clienteId\":", "\"valorTotal\":0.01,\"clienteId\":"), 400);
    }

    @Test
    void rejeitaEmailDuplicadoMesmoComMaiusculasOuEspacos() throws Exception {
        for (String email : new String[]{"ana@example.com", "ANA@EXAMPLE.COM", "  Ana@Example.com  "}) {
            String erro = enviar("POST", "/api/clientes", clienteJson(email), 409);
            assertThat((String) JsonPath.read(erro, "$.detail"))
                .isEqualTo("Ja existe um cliente cadastrado com este e-mail.");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cupcake.tb_clientes", Integer.class)).isEqualTo(1);
    }

    @Test
    void normalizaEmailAntesDeValidarEPersistir() throws Exception {
        String cadastrado = enviar("POST", "/api/clientes", clienteJson("  Maria@Example.com  "), 200);
        assertThat((String) JsonPath.read(cadastrado, "$.email")).isEqualTo("maria@example.com");
        assertThat(jdbc.queryForObject("SELECT email FROM cupcake.tb_clientes WHERE id = ?",
            String.class, id(cadastrado))).isEqualTo("maria@example.com");
    }

    @Test
    void atualizacaoAceitaProprioEmailERejeitaEmailDeOutroCliente() throws Exception {
        enviar("PUT", "/api/clientes/" + cliente, clienteJson(" ANA@EXAMPLE.COM "), 200);
        long outro = id(enviar("POST", "/api/clientes", clienteJson("maria@example.com"), 200));
        enviar("PUT", "/api/clientes/" + outro, clienteJson(" ANA@EXAMPLE.COM "), 409);
        assertThat((String) JsonPath.read(enviar("GET", "/api/clientes/" + outro, null, 200), "$.email"))
            .isEqualTo("maria@example.com");
    }

    @Test
    void bancoImpedeDuplicidadeEEmailNaoNormalizado() {
        String sql = "INSERT INTO cupcake.tb_clientes (nome, email, telefone) VALUES (?, ?, ?)";
        assertThatThrownBy(() -> jdbc.update(sql, "Outra Ana", "ana@example.com", "71999999999"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(sql, "Outra Ana", " ANA@EXAMPLE.COM ", "71999999999"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void requisicoesSimultaneasNaoCriamDoisClientesComMesmoEmail() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/clientes"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(clienteJson("concorrente@example.com")))
            .build();
        var primeira = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var segunda = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        assertThat(java.util.List.of(
            primeira.get(10, java.util.concurrent.TimeUnit.SECONDS).statusCode(),
            segunda.get(10, java.util.concurrent.TimeUnit.SECONDS).statusCode()
        )).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM cupcake.tb_clientes WHERE email = ?",
            Integer.class,
            "concorrente@example.com"
        )).isEqualTo(1);
    }

    String clienteJson(String email) {
        return """
            {"nome":"Cliente","email":"%s","telefone":"71999999999"}
            """.formatted(email);
    }

    long criarPedido() throws Exception {
        return id(enviar("POST", "/api/pedidos",
            pedidoJson("[{\"cupcakeId\":" + cupcake + ",\"quantidade\":2}]"), 201));
    }

    String pedidoJson(String itens) {
        return """
            {"clienteId":%d,"itens":%s,"endereco":{"logradouro":"Rua A","numero":"10",
            "bairro":"Centro","cidade":"Salvador","estado":"BA","cep":"40000-000"}}
            """.formatted(cliente, itens);
    }

    String pagar(long pedido, String forma, int status) throws Exception {
        return enviar("POST", "/api/pedidos/" + pedido + "/pagamento-simulado",
            "{\"formaPagamento\":\"" + forma + "\"}", status);
    }

    long id(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    String enviar(String metodo, String rota, String body, int status) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + rota))
            .header("Content-Type", "application/json")
            .method(metodo, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).withFailMessage("%s %s: %s", metodo, rota, response.body()).isEqualTo(status);
        return response.body();
    }
}
