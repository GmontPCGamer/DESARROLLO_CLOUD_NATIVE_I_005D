package com.cloudnative.payment.messaging;

import java.io.IOException;

import org.slf4j.Logger;

import org.springframework.amqp.core.Message;

import com.rabbitmq.client.Channel;

/**
 * ACK manual. Un mensaje válido se confirma. Uno inválido se rechaza sin
 * requeue para que la DLX lo deje en la DLQ. Un error recuperable se reencola
 * una vez; si ya venía reentregado, también va a la DLQ.
 */
final class DeliveryControl {

  private DeliveryControl() {
  }

  static void ack(Channel channel, long tag) throws IOException {
    channel.basicAck(tag, false);
  }

  static void rejectToDlq(Channel channel, long tag, Logger log, String reason) throws IOException {
    log.error("NACK sin requeue, el mensaje va a la DLQ: {}", reason);
    channel.basicNack(tag, false, false);
  }

  static void retryOrDlq(Channel channel, Message message, Logger log, String reason) throws IOException {
    long tag = message.getMessageProperties().getDeliveryTag();
    if (message.getMessageProperties().isRedelivered()) {
      log.error("Reintento agotado. NACK sin requeue, el mensaje va a la DLQ: {}", reason);
      channel.basicNack(tag, false, false);
      return;
    }
    log.warn("Error recuperable. NACK con requeue (un reintento): {}", reason);
    channel.basicNack(tag, false, true);
  }
}
