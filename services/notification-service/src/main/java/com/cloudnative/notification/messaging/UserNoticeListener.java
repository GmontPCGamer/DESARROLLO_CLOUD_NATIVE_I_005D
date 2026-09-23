package com.cloudnative.notification.messaging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.cloudnative.notification.domain.Notification;
import com.cloudnative.notification.repo.NotificationRepository;

@Component
public class UserNoticeListener {

  private static final Logger log = LoggerFactory.getLogger(UserNoticeListener.class);

  private final NotificationRepository notifications;

  public UserNoticeListener(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @RabbitListener(queues = RabbitConfig.NOTIFICATIONS)
  public void onNotice(UserNotice event) {
    if (event.getUserId() == null || event.getTitle() == null || event.getMessage() == null) {
      log.warn("Aviso incompleto, se descarta");
      return;
    }
    Notification notification = new Notification();
    notification.setUserId(event.getUserId());
    notification.setTitle(event.getTitle());
    notification.setMessage(event.getMessage());
    notification.setType(event.getType() == null ? "SYSTEM" : event.getType());
    notification.setRead(false);
    notification.setCreatedAt(Instant.now());
    notifications.save(notification);
    log.info("Aviso persistido type={} userId={}", notification.getType(), event.getUserId());
  }
}
