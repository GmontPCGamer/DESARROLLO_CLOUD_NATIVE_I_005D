package com.cloudnative.payment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.cloudnative.payment.web.PaymentLedger;

@Component
public class PaymentCaptureListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentCaptureListener.class);

  private final PaymentLedger ledger;

  public PaymentCaptureListener(PaymentLedger ledger) {
    this.ledger = ledger;
  }

  @RabbitListener(queues = RabbitConfig.PAYMENTS)
  public void onOrderPlaced(OrderPlacedEvent event) {
    if (event.getPaymentId() == null || event.getPaymentId().isBlank()) {
      return;
    }
    boolean captured = ledger.capture(event.getPaymentId(), event.getUserId());
    if (captured) {
      log.info("Pago {} capturado para la orden {}", event.getPaymentId(), event.getOrderId());
    } else {
      log.warn("No se capturó el pago {} de la orden {}", event.getPaymentId(), event.getOrderId());
    }
  }
}
