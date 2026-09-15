package com.cloudnative.backend.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.json.JsonMapper;

/**
 * Traduce los errores del BFF a respuestas RFC 7807 (ProblemDetail).
 *
 * - {@link RestClientResponseException}: un microservicio respondió 4xx/5xx → se
 *   propaga el mismo código y el {@code detail} original (ej. 409 "Stock insuficiente").
 * - {@link ResourceAccessException}: el microservicio no responde → 503.
 * - {@code ResponseStatusException} y errores de binding los gestiona la clase base.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @ExceptionHandler(RestClientResponseException.class)
  ProblemDetail downstreamError(RestClientResponseException ex, WebRequest request) {
    HttpStatusCode status = ex.getStatusCode();
    String detail = detailFrom(ex.getResponseBodyAsString());

    if (status.value() == 401 || status.value() == 403) {
      // El token del usuario fue aceptado por el BFF pero rechazado por un servicio interno:
      // es un problema de configuración entre servicios, no del cliente.
      log.warn("Servicio interno rechazó el token ({}): {}", status.value(), ex.getResponseBodyAsString());
      return problem(HttpStatus.BAD_GATEWAY,
          "Un servicio interno rechazó la credencial del usuario (" + status.value() + ")", request);
    }
    if (status.is5xxServerError()) {
      log.error("Servicio interno falló ({}): {}", status.value(), ex.getResponseBodyAsString());
      return problem(HttpStatus.BAD_GATEWAY,
          detail != null ? detail : "Un servicio interno falló al procesar la solicitud", request);
    }
    return problem(status, detail != null ? detail : "La solicitud fue rechazada (" + status.value() + ")", request);
  }

  @ExceptionHandler(ResourceAccessException.class)
  ProblemDetail downstreamUnavailable(ResourceAccessException ex, WebRequest request) {
    log.warn("Servicio interno no disponible: {}", ex.getMessage());
    return problem(HttpStatus.SERVICE_UNAVAILABLE,
        "Un servicio interno no está disponible. Verifica que todos los microservicios estén levantados.", request);
  }

  private static ProblemDetail problem(HttpStatusCode status, String detail, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    HttpStatus resolved = HttpStatus.resolve(status.value());
    problem.setTitle(resolved != null ? resolved.getReasonPhrase() : "Error");
    String path = request.getDescription(false);
    if (path.startsWith("uri=")) {
      problem.setInstance(URI.create(path.substring(4)));
    }
    return problem;
  }

  private static String detailFrom(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      ProblemDetail problem = JSON.readValue(body, ProblemDetail.class);
      return problem.getDetail();
    } catch (Exception ignored) {
      return null;
    }
  }
}
