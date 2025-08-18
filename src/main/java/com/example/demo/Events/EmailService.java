package com.example.demo.Events;

import com.example.demo.client.domain.Client;
import com.example.demo.reunion.domain.Reunion;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendReunionConfirmation(Reunion reunion, Client client) throws MessagingException{

            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("sessionDate", reunion.getSessionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            context.setVariable("sessionTag", reunion.getTag());
            context.setVariable("sessionDescription", reunion.getDescription());
            context.setVariable("meetingUrl", reunion.getUrl());
            context.setVariable("cost", reunion.getCost());

            String process = templateEngine.process("EventCalendarNotification.html", context);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(client.getEmail());
            helper.setSubject("Confirmación de Sesión de - " + reunion.getTag());
            helper.setText(process, true);
            javaMailSender.send(message);

    }
}
