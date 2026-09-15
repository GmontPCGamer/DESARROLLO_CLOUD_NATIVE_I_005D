package com.cloudnative.notification.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cloudnative.notification.domain.Notification;
import com.cloudnative.notification.repo.NotificationRepository;

@RestController
@RequestMapping("/api")
public class NotificationController {

  private final NotificationRepository notifications;

  public NotificationController(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "notification-service");
  }

  @GetMapping("/notifications")
  public List<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return notifications.findByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/notifications/unread-count")
  public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
    return Map.of("count", notifications.countByUserIdAndReadFalse(jwt.getSubject()));
  }

  @PostMapping("/notifications")
  public NotificationResponse create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRequest request) {
    if (request.title() == null || request.message() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título y mensaje son obligatorios");
    }
    Notification notification = new Notification();
    notification.setUserId(jwt.getSubject());
    notification.setTitle(request.title());
    notification.setMessage(request.message());
    notification.setType(request.type() == null ? "SYSTEM" : request.type());
    notification.setRead(false);
    notification.setCreatedAt(Instant.now());
    return toResponse(notifications.save(notification));
  }

  @PatchMapping("/notifications/read-all")
  public Map<String, Long> markAllRead(@AuthenticationPrincipal Jwt jwt) {
    List<Notification> pending = notifications.findByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
        .filter(notification -> !notification.isRead())
        .toList();
    pending.forEach(notification -> notification.setRead(true));
    notifications.saveAll(pending);
    return Map.of("updated", (long) pending.size());
  }

  @PatchMapping("/notifications/{id}/read")
  public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    Notification notification = notifications.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
    if (!notification.getUserId().equals(jwt.getSubject())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar esta notificación");
    }
    notification.setRead(true);
    return toResponse(notifications.save(notification));
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getType(),
        notification.isRead(),
        notification.getCreatedAt());
  }

  public record CreateRequest(String title, String message, String type) {
  }

  public record NotificationResponse(
      Long id,
      String title,
      String message,
      String type,
      boolean read,
      Instant createdAt) {
  }
}
