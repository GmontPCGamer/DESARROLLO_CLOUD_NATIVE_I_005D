package com.cloudnative.rabbitadmin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BindingRequest(
    @NotBlank(message = "la cola es obligatoria")
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "nombre de cola inválido")
    String queue,
    @NotBlank(message = "el exchange es obligatorio")
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "nombre de exchange inválido")
    String exchange,
    @NotBlank(message = "la routing key es obligatoria")
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "routing key inválida")
    String routingKey) {
}
