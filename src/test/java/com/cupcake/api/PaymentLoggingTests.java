package com.cupcake.api;

import com.cupcake.api.shared.RequestLoggingFilter;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class PaymentLoggingTests {
    @Test
    void correlacionaSucessoRejeicaoEFalhaSemRegistrarCorpo() throws Exception {
        var logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(RequestLoggingFilter.class);
        List<LogEvent> events = new ArrayList<>();
        var appender = new AbstractAppender("payment-test", null, null, false, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent event) { events.add(event.toImmutable()); }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            for (int status : new int[]{200, 400, 409, 500}) {
                events.clear();
                var request = new MockHttpServletRequest("POST", "/api/pedidos/42/pagamento-simulado");
                request.setContent("{\"numeroCartao\":\"SEGREDO\"}".getBytes());
                var response = new MockHttpServletResponse();
                var filter = new RequestLoggingFilter();
                jakarta.servlet.FilterChain chain = (req, res) -> {
                    if (status == 500) throw new IllegalStateException("SEGREDO");
                    ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status);
                };
                if (status == 500) {
                    assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(IllegalStateException.class);
                } else {
                    filter.doFilter(request, response, chain);
                }
                assertThat(response.getHeader("X-Request-Id")).isNotBlank();
                assertThat(events).allSatisfy(event -> {
                    assertThat(event.getContextData().<String>getValue("requestId")).isEqualTo(response.getHeader("X-Request-Id"));
                    assertThat(event.getMessage().getFormattedMessage()).doesNotContain("SEGREDO", "numeroCartao");
                });
                assertThat(events.getLast().getMessage().getFormattedMessage()).contains("pedidoId=42", "status=" + status);
                assertThat(ThreadContext.get("requestId")).isNull();
            }
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
    }
}
