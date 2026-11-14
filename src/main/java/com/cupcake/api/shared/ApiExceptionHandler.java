package com.cupcake.api.shared;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ProblemDetail> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ProblemDetail> regra(RegraNegocioException e) {
        return erro(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException e) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos.");
        detalhe.setProperty("campos", e.getBindingResult().getFieldErrors().stream()
            .map(f -> Map.of("campo", f.getField(), "mensagem", String.valueOf(f.getDefaultMessage()))).toList());
        return ResponseEntity.badRequest().body(detalhe);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> json(HttpMessageNotReadableException e) {
        return erro(HttpStatus.BAD_REQUEST, "JSON invalido ou valor nao aceito. Pagamento aceita apenas CREDITO ou DEBITO.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concorrencia(ObjectOptimisticLockingFailureException e) {
        return erro(HttpStatus.CONFLICT, "Pedido alterado por outra operacao. Consulte novamente.");
    }

    private ResponseEntity<ProblemDetail> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, mensagem));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridade(DataIntegrityViolationException e) {
        return erro(HttpStatus.CONFLICT, "Dados em conflito com um cadastro existente. Verifique o e-mail informado.");
    }
}
