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

    public void enviarNotificacionPlanificacionAprobada(String correoDestinatario, String cursoPlanificacion, String ejeMatematico) {
        try {
            SimpleMailMessage mensajeCorreo = new SimpleMailMessage();
            mensajeCorreo.setTo(correoDestinatario);
            mensajeCorreo.setSubject("Planificación Aprobada - Plataforma de Prácticas UBB");

            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            String cursoTexto = cursoPlanificacion != null ? cursoPlanificacion : "No especificado";
            String ejeTexto = ejeMatematico != null && !ejeMatematico.isBlank() ? ejeMatematico : "General";

            mensajeCorreo.setText("Estimado estudiante,\n\n" +
                    "Te informamos que tu planificación didáctica ha sido aprobada por tu docente.\n\n" +
                    "DETALLES DE LA PLANIFICACIÓN:\n" +
                    "- Curso: " + cursoTexto + "\n" +
                    "- Eje Matemático: " + ejeTexto + "\n" +
                    "- Fecha de aprobación: " + fechaActual + "\n" +
                    "- Estado: APROBADA (Lista para aplicar en el colegio)\n\n" +
                    "Ya puedes aplicar esta planificación en tu centro de práctica.\n\n" +
                    "Saludos,\nPlataforma de Prácticas UBB");

            mailSender.send(mensajeCorreo);
        } catch (Exception error) {
            System.err.println("Error enviando correo de planificación aprobada: " + error.getMessage());
        }
    }
}