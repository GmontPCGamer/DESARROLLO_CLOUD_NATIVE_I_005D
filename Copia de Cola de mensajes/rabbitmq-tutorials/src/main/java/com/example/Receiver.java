package com.example;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Consumidor (Receiver) que recibe mensajes de la cola "hello".
 *
 * Basado en:
 * https://www.rabbitmq.com/tutorials/tutorial-one-spring-amqp
 * https://docs.spring.io/spring-amqp/reference/
 */
@Component
public class Receiver {

    /**
     * Se ejecuta automáticamente cada vez que llega un mensaje a la cola "hello".
     */
    @RabbitListener(queues = "hello")
    public void receiveMessage(String message) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            System.out.println("[" + timestamp + "] [✓] Mensaje recibido: '" + message + "'");
        } catch (Exception e) {
            System.err.println("[✗] Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
