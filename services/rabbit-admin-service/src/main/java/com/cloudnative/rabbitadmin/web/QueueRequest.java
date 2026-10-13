package com.cloudnative.rabbitadmin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta de una cola. El nombre vacío o con caracteres raros se rechaza. */
public record QueueRequest(
    @NotBlank(message = "el nombre de la cola es obligatorio")
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "el nombre solo admite letras, números, punto, guion y guion bajo")
    String name,
    Boolean durable,
    @Size(max = 255)
    @Pattern(regexp = "^$|^[A-Za-z0-9._-]+$", message = "deadLetterExchange inválido")
    String deadLetterExchange,
    @Size(max = 255)
    @Pattern(regexp = "^$|^[A-Za-z0-9._-]+$", message = "deadLetterRoutingKey inválida")
    String deadLetterRoutingKey) {
}
