package com.example.demo;

import com.example.demo.exceptions.ResourceAlreadyExists;
import com.example.demo.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException(ResourceNotFoundException ex){
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResourceAlreadyExists.class)
    public String handleResourceAlreadyExistsException(ResourceAlreadyExists ex){
        return ex.getMessage();
    }
}
