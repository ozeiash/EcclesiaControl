package br.com.bitabit.ecclesiacontrol.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDetail> fieldErrors; // só preenchido em erro de validação

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private String field;
        private String message;
    }
}