package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller para enviar mensajes a RabbitMQ.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private Sender sender;

    /**
     * Enviar un mensaje via POST.
     *
     * curl -X POST http://localhost:8080/api/messages \
     *   -H "Content-Type: application/json" \
     *   -d '{"message":"Hello from REST API!"}'
     */
    @PostMapping
    public ResponseEntity<String> sendMessage(@RequestBody MessageRequest request) {
        try {
            sender.sendMessage(request.getMessage());
            return ResponseEntity.ok("Mensaje enviado: " + request.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Enviar un mensaje via GET.
     *
     * curl "http://localhost:8080/api/messages/send?message=Hello%20World"
     */
    @GetMapping("/send")
    public ResponseEntity<String> sendMessageGet(@RequestParam(name = "message") String message) {
        try {
            sender.sendMessage(message);
            return ResponseEntity.ok("Mensaje enviado: " + message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    public static class MessageRequest {
        private String message;

        public MessageRequest() {
        }

        public MessageRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
