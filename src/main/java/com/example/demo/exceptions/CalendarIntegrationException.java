package com.example.demo.exceptions;

public class CalendarIntegrationException extends RuntimeException {
    public CalendarIntegrationException(String message) {
        super(message);
    }
    
    public CalendarIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
