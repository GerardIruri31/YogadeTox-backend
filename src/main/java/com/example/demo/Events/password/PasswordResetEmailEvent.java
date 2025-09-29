package com.example.demo.Events.password;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PasswordResetEmailEvent  extends ApplicationEvent {
        private final String emailDestino;
        private final String enlace; // URL con el token
    public PasswordResetEmailEvent(Object source, String emailDestino, String enlace) {
        super(source);
        this.emailDestino = emailDestino;
        this.enlace = enlace;
    }
}


