package com.example.demo.Events.password;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.example.demo.Events.service.EmailService;

@Component
@RequiredArgsConstructor
public class PasswordEmailListener {

    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @EventListener
    public void onReset(PasswordResetEmailEvent e) {
        Context ctx = new Context();
        ctx.setVariable("token", e.getEnlace());
        String html = templateEngine.process("PasswordReset.html", ctx);
        emailService.sendHtml(e.getEmailDestino(), "Recupera tu contraseña", html);
    }

    @EventListener
    public void onChanged(PasswordChangedEmailEvent e) {
        Context ctx = new Context();
        String html = templateEngine.process("PasswordChanged.html", ctx);
        emailService.sendHtml(e.getEmailDestino(), "Tu contraseña fue cambiada", html);
    }
}