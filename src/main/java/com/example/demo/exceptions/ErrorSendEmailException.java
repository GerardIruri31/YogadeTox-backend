package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ErrorSendEmailException extends RuntimeException{
    public ErrorSendEmailException(String message){
        super(message);
    }
}