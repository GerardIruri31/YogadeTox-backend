package com.example.demo.Events.EventsCalendarNotification;

import com.example.demo.Events.EmailService;
import com.example.demo.exceptions.ErrorSendEmailException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventNotificationEventListener {
    
    private final EmailService emailService;

    @EventListener
    @Async
    public void handleReunionConfirmation(EventsNotificationEvent event) {
        try{
            emailService.sendReunionConfirmation(event.getReunion(), event.getClient());
        } catch (MessagingException e){
            throw new ErrorSendEmailException("Fail to send email to " + event.getClient().getEmail());
        }
    }
}
