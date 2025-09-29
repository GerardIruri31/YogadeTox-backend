package com.example.demo.Events.password;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PasswordChangedEmailEvent extends ApplicationEvent {
    private final String emailDestino;
    public PasswordChangedEmailEvent(Object source, String emailDestino) {
        super(source);
        this.emailDestino = emailDestino;

    }

}

