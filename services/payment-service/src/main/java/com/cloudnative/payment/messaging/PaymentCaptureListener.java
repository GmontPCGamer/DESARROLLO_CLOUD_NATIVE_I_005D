package com.cloudnative.payment.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.cloudnative.payment.web.PaymentLedger;
import com.rabbitmq.client.Channel;

@Component
public class PaymentCaptureListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentCaptureListener.class);

  private final PaymentLedger ledger;

  public PaymentCaptureListener(PaymentLedger ledger) {
    this.ledger = ledger;
  }

  @RabbitListener(queues = RabbitConfig.PAYMENTS)
  public void onOrderPlaced(
      OrderPlacedEvent event,
      Channel channel,
      Message message,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    if (event == null || event.getPaymentId() == null || event.getPaymentId().isBlank()) {
      DeliveryControl.rejectToDlq(channel, tag, log, "compra sin paymentId");
      return;
    }
    try {
      boolean captured = ledger.capture(event.getPaymentId(), event.getUserId());
      if (!captured) {
        DeliveryControl.retryOrDlq(channel, message, log,
            "no se capturó el pago " + event.getPaymentId() + " de la orden " + event.getOrderId());
        return;
      }
      DeliveryControl.ack(channel, tag);
      log.info("ACK pago {} capturado para la orden {}", event.getPaymentId(), event.getOrderId());
    } catch (RuntimeException ex) {
      DeliveryControl.retryOrDlq(channel, message, log, ex.getMessage());
    }
  }
}
