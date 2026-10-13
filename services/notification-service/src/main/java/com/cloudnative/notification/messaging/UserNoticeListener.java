package com.cloudnative.notification.messaging;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.cloudnative.notification.domain.Notification;
import com.cloudnative.notification.repo.NotificationRepository;
import com.rabbitmq.client.Channel;

@Component
public class UserNoticeListener {

  private static final Logger log = LoggerFactory.getLogger(UserNoticeListener.class);

  private final NotificationRepository notifications;

  public UserNoticeListener(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @RabbitListener(queues = RabbitConfig.NOTIFICATIONS)
  public void onNotice(
      UserNotice event,
      Channel channel,
      Message message,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    if (event == null || event.getUserId() == null || event.getTitle() == null || event.getMessage() == null) {
      DeliveryControl.rejectToDlq(channel, tag, log, "aviso incompleto");
      return;
    }
    try {
      Notification notification = new Notification();
      notification.setUserId(event.getUserId());
      notification.setTitle(event.getTitle());
      notification.setMessage(event.getMessage());
      notification.setType(event.getType() == null ? "SYSTEM" : event.getType());
      notification.setRead(false);
      notification.setCreatedAt(Instant.now());
      notifications.save(notification);
      DeliveryControl.ack(channel, tag);
      log.info("ACK aviso persistido type={} userId={}", notification.getType(), event.getUserId());
    } catch (RuntimeException ex) {
      DeliveryControl.retryOrDlq(channel, message, log, ex.getMessage());
    }
  }
}
