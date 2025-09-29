package com.example.demo.Events.welcome;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WelcomeEmailEvent extends ApplicationEvent {
    private final String username;
    private final String to;
    public WelcomeEmailEvent(Object source, String username, String to) {
        super(source);
        this.username = username;
        this.to = to;
    }

}
