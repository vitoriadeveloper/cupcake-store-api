package com.cupcake.api;

import com.cupcake.api.pagamento.PagamentoLocalService;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PagamentoDesabilitadoIntegrationTests {
    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    @Test
    void endpointEServicoNaoExistemForaDoPerfilLocal() throws Exception {
        assertThat(context.getBeansOfType(PagamentoLocalService.class)).isEmpty();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/pedidos/1/pagamento-simulado"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"formaPagamento\":\"CREDITO\"}")).build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
