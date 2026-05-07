package br.com.fiap.techchallenge.oficina.interfaces.rest.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        List<ApiError.FieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(extrairCampo(v.getPropertyPath().toString()),
                        v.getMessage()))
                .toList();
        ApiError body = ApiError.of(400, "Bad Request",
                "Falha de validação nos campos da requisição", fields);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }

    /** {@code cadastrar.arg0.documento} → {@code documento} */
    private static String extrairCampo(String path) {
        int idx = path.lastIndexOf('.');
        return idx < 0 ? path : path.substring(idx + 1);
    }
}
