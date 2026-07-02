package br.com.fiap.techchallenge.oficina.external.api.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fields
) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(OffsetDateTime.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<FieldError> fields) {
        return new ApiError(OffsetDateTime.now(), status, error, message, fields);
    }

    public record FieldError(String field, String message) {}
}
