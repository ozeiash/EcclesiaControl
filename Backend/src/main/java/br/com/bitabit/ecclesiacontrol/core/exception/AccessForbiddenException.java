package br.com.bitabit.ecclesiacontrol.core.exception;

import org.springframework.http.HttpStatus;

public class AccessForbiddenException extends ApiException {
    public AccessForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}