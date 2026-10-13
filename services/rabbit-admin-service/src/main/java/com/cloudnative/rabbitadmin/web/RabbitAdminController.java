package com.cloudnative.rabbitadmin.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cloudnative.rabbitadmin.service.RabbitAdminService;

import jakarta.validation.Valid;

/**
 * API de administración de RabbitMQ.
 * POST /queues, DELETE /queues/{name}, y lo mismo para exchanges y bindings.
 */
@RestController
public class RabbitAdminController {

  private final RabbitAdminService admin;

  public RabbitAdminController(RabbitAdminService admin) {
    this.admin = admin;
  }

  @PostMapping("/queues")
  public ResponseEntity<Map<String, String>> createQueue(@Valid @RequestBody QueueRequest request) {
    admin.createQueue(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("name", request.name(), "status", "created"));
  }

  @GetMapping("/queues/{name}")
  public Map<String, String> getQueue(@PathVariable String name) {
    return Map.of("name", name, "properties", admin.queue(name).toString());
  }

  @DeleteMapping("/queues/{name}")
  public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
    admin.deleteQueue(name);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/exchanges")
  public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody ExchangeRequest request) {
    admin.createExchange(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("name", request.name(), "status", "created"));
  }

  @DeleteMapping("/exchanges/{name}")
  public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
    admin.deleteExchange(name);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/bindings")
  public ResponseEntity<Map<String, String>> createBinding(@Valid @RequestBody BindingRequest request) {
    admin.createBinding(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "queue", request.queue(),
        "exchange", request.exchange(),
        "routingKey", request.routingKey(),
        "status", "created"));
  }

  @DeleteMapping("/bindings")
  public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequest request) {
    admin.deleteBinding(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }
}
