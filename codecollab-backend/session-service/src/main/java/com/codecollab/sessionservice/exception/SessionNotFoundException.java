package com.codecollab.sessionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// This powerful annotation tells Spring to automatically return a 404 NOT_FOUND status
// whenever this exception is thrown from a controller.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }
}