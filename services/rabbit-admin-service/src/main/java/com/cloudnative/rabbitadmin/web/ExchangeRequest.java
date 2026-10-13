package com.cloudnative.rabbitadmin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(
    @NotBlank(message = "el nombre del exchange es obligatorio")
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "el nombre solo admite letras, números, punto, guion y guion bajo")
    String name,
    @NotBlank(message = "el tipo de exchange es obligatorio")
    @Pattern(regexp = "direct|topic|fanout|headers", message = "tipo permitido: direct, topic, fanout o headers")
    String type,
    Boolean durable) {
}
