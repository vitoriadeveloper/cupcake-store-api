package com.cupcake.api.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Log4j2
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().matches("/api/pedidos/[^/]+/pagamento-simulado");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String rawId = request.getRequestURI().split("/")[3];
        String pedidoId = rawId.matches("[0-9]{1,19}") ? rawId : "invalido";
        long inicio = System.nanoTime();
        boolean falhou = false;
        try (var context = org.apache.logging.log4j.CloseableThreadContext.put("requestId", requestId)) {
            response.setHeader("X-Request-Id", requestId);
            log.info("evento=pagamento_requisicao_recebida pedidoId={} metodo={}", pedidoId, request.getMethod());
            try {
                chain.doFilter(request, response);
            } catch (ServletException | IOException | RuntimeException ex) {
                falhou = true;
                log.error("evento=pagamento_erro_inesperado pedidoId={} tipo={}", pedidoId, ex.getClass().getSimpleName());
                throw ex;
            } finally {
                int status = falhou ? 500 : response.getStatus();
                long duracao = (System.nanoTime() - inicio) / 1_000_000;
                if (status >= 500) log.error("evento=pagamento_requisicao_falhou pedidoId={} status={} duracaoMs={}", pedidoId, status, duracao);
                else if (status >= 400) log.warn("evento=pagamento_requisicao_rejeitada pedidoId={} status={} duracaoMs={}", pedidoId, status, duracao);
                else log.info("evento=pagamento_requisicao_concluida pedidoId={} status={} duracaoMs={}", pedidoId, status, duracao);
            }
        }
    }
}
