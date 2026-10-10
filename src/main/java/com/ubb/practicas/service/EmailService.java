package com.ubb.practicas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarComprobante(String destinatario, String nombreArchivo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("Comprobante de Entrega - Portafolio UBB");

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            mensaje.setText("Estimado estudiante,\n\n" +
                    "Este correo automatizado confirma que hemos recibido exitosamente tu entrega.\n\n" +
                    "DETALLES DE LA ENTREGA:\n" +
                    "- Documento: " + nombreArchivo + "\n" +
                    "- Fecha y Hora: " + fecha + "\n" +
                    "- Estado: RECIBIDO CON ÉXITO\n\n" +
                    "Por favor, guarda este correo como respaldo oficial de tu entrega.\n\n" +
                    "Saludos,\nPlataforma de Prácticas UBB");

            mailSender.send(mensaje);
            System.out.println("Comprobante enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error enviando correo: " + e.getMessage());
        }
    }
}