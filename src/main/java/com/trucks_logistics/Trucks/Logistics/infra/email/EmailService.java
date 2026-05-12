package com.trucks_logistics.Trucks.Logistics.infra.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCodigoVerificacion(String emailDestino, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailDestino);
            helper.setFrom("trazo@gmail.com");
            helper.setSubject("Verifica tu cuenta - Trucks Logistics");

            // Creamos el cuerpo del correo con HTML
            String htmlContent = """
                    <div style="font-family: Arial; background-color: #f4f4f4; padding: 20px;">
                        <div style="background-color: #ffffff; padding: 40px; border-radius: 10px; text-align: center; max-width: 500px; margin: auto;">
                            <h2 style="color: #333;">Verifica tu cuenta en Trazo 🚛</h2>
                            <p>Gracias por registrarte. Haz click en el botón para verificar tu cuenta:</p>
                            <a href="%s" style="display: inline-block; background-color: #333; color: white; padding: 12px 24px; border-radius: 5px; text-decoration: none; margin: 20px 0;">
                                Verificar mi cuenta
                            </a>
                            <p style="font-size: 13px; color: #666;">Este link expira en 24 horas.</p>
                            <p style="font-size: 13px; color: #666;">Si no creaste esta cuenta ignora este email.</p>
                        </div>
                    </div>
                    """
                    .formatted(verificationLink);

            helper.setText(htmlContent, true); // El 'true' es vital: indica que es HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo", e);
        }
    }
}
