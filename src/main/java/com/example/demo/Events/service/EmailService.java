package com.example.demo.Events.service;

import com.example.demo.client.domain.Client;
import com.example.demo.reunion_temp.domain.Reunion;
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



    @Async
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage mime = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML
            javaMailSender.send(mime);
        } catch (Exception ex) {
            throw new RuntimeException("Error enviando email", ex);
        }
    }


    @Async("welcomeEventExecutor")
    public void sendEmail(String username, String to) {
        String asunto = "¡Bienvenido a YogaDetox, " + username + "!";
        String html = """
       <!doctype html>
                   <html lang="es">
                     <head>
                       <meta charset="utf-8">
                       <title>Bienvenido a YogaDetox</title>
                       <meta name="viewport" content="width=device-width, initial-scale=1">
                       <!-- Preheader -->
                       <meta name="x-preheader" content="Accede a videocursos, meditaciones y reserva sesiones privadas. Empieza ahora.">
                       <style>
                         @media only screen and (max-width: 620px) {
                           .container { width: 100% !important; }
                           .btn { display:block !important; width:100% !important; }
                           .stack { display:block !important; width:100% !important; }
                         }
                       </style>
                     </head>
                     <body style="margin:0;padding:0;background:#f6f9fc;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                       <span style="display:none!important;visibility:hidden;opacity:0;height:0;width:0;overflow:hidden;">
                         Accede a videocursos, meditaciones y reserva sesiones privadas. Empieza ahora.
                       </span>
                
                       <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f6f9fc;padding:24px 0;">
                         <tr>
                           <td align="center">
                             <table role="presentation" class="container" width="600" cellspacing="0" cellpadding="0" style="width:600px;max-width:600px;background:#ffffff;border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,.05);overflow:hidden;">
                               <!-- Header -->
                               <tr>
                                 <td style="background:linear-gradient(135deg,#6ee7b7,#93c5fd);padding:28px 24px;text-align:center;">
                                   <h1 style="margin:0;font-size:24px;line-height:1.2;color:#0b2534;">🧘 YogaDetox</h1>
                                   <p style="margin:8px 0 0 0;font-size:14px;color:#0b2534;opacity:.9;">Bienvenido a tu espacio de calma y progreso</p>
                                 </td>
                               </tr>
                
                               <!-- Body -->
                               <tr>
                                 <td style="padding:24px;">
                                   <h2 style="margin:0 0 12px 0;font-size:20px;color:#111827;">
                                     ¡Hola, <span style="color:#0ea5e9;">${username}</span>! 👋
                                   </h2>
                                   <p style="margin:0 0 12px 0;font-size:15px;line-height:1.6;color:#374151;">
                                     Gracias por unirte a <strong>YogaDetox</strong>. Desde hoy tienes acceso a:
                                   </p>
                
                                   <ul style="padding-left:18px;margin:0 0 16px 0;font-size:15px;line-height:1.7;color:#374151;">
                                     <li>🎥 <strong>Videocursos</strong> de yoga (libres y <em>premium</em>).</li>
                                     <li>🎧 <strong>Audios de meditación</strong> para cualquier momento del día.</li>
                                     <li>📅 <strong>Reserva de sesiones privadas</strong> con especialistas.</li>
                                     <li>💬 <strong>Chat directo</strong> con el administrador para resolver dudas.</li>
                                     <li>🌐 <strong>Multilenguaje</strong> (Español / Inglés) — cámbialo cuando quieras.</li>
                                     <li>💳 Pagos seguros con <strong>Stripe</strong> y <strong>MercadoPago</strong> para activar Premium.</li>
                                   </ul>
                                   <hr style="border:none;border-top:1px solid #e5e7eb;margin:20px 0;">
                                   <p style="margin:0 0 10px 0;font-size:14px;line-height:1.6;color:#4b5563;">
                                     ¿Primera vez aquí? Te recomendamos empezar por el <strong>Programa Inicial</strong> para aprender posturas básicas y respiración.
                                   </p>
                                   <p style="margin:0 0 10px 0;font-size:14px;color:#4b5563;">
                                     Si cierras la app, al volver podrás reanudar tus <em>videos y audios</em> exactamente donde te quedaste.
                                   </p>
                
                                   <p style="margin:16px 0 0 0;font-size:12px;color:#6b7280;">
                                     Si no creaste esta cuenta, por favor contáctanos de inmediato.
                                   </p>
                                 </td>
                               </tr>
                
                               <!-- Footer -->
                             
                                           <tr>
                                             <td style="background:#f9fafb;padding:28px 24px;text-align:center;border-top:1px solid #e5e7eb;">
                                               <p style="margin:0 0 8px 0;font-size:13px;color:#4b5563;line-height:1.6;">
                                                 Estás recibiendo este correo porque creaste una cuenta en <strong>YogaDetox</strong>.
                                               </p>
                                               <p style="margin:0 0 12px 0;font-size:13px;color:#4b5563;line-height:1.6;">
                                                 Mantente conectado a tu espacio de calma y bienestar 🌿
                                               </p>
                                               <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                                                 © 2025 YogaDetox · Todos los derechos reservados
                                               </p>
                                               <p style="margin:12px 0 0 0;font-size:12px;color:#6b7280;line-height:1.6;">
                                                 Preferencias &nbsp;|&nbsp; Soporte &nbsp;|&nbsp; Darse de baja
                                               </p>
                                             </td>
                                           </tr>
                             </table>
                           </td>
                         </tr>
                       </table>
                     </body>
                   </html>
                
    """.replace("${username}", username);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(asunto);
            helper.setText(html, true); // ⚠️ true para que se renderice como HTML
            javaMailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar correo a " + to + ": " + e.getMessage());
        }


    }


}