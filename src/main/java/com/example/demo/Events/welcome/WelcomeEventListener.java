package com.example.demo.Events.welcome;

import com.example.demo.Events.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WelcomeEventListener {
    @Autowired
    private EmailService emailService;

    @Async("welcomeEventExecutor")
    @EventListener
    public void sendWelcomeEmail(WelcomeEmailEvent welcomeEmailEvent) {
        emailService.sendEmail(welcomeEmailEvent.getUsername(), welcomeEmailEvent.getTo());
    }



}
