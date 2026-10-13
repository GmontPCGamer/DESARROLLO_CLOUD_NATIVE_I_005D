package com.cloudnative.messaging;

import java.util.Set;

/**
 * Único lugar con los nombres de la topología. Los microservicios referencian
 * estas constantes; no vuelven a escribir el texto.
 */
public final class MessagingNames {

  public static final String NOTIFICATIONS = "nexotech.notifications";
  public static final String NOTIFICATIONS_EXCHANGE = "nexotech.notifications.exchange";
  public static final String NOTIFICATIONS_ROUTING_KEY = "notification.user";
  public static final String NOTIFICATIONS_DLX = "nexotech.notifications.dlx";
  public static final String NOTIFICATIONS_DLQ = "nexotech.notifications.dlq";

  public static final String SHIPMENTS = "nexotech.shipments";
  public static final String SHIPMENTS_EXCHANGE = "nexotech.shipments.exchange";
  public static final String SHIPMENTS_ROUTING_KEY = "shipment.create";
  public static final String SHIPMENTS_DLX = "nexotech.shipments.dlx";
  public static final String SHIPMENTS_DLQ = "nexotech.shipments.dlq";

  public static final String PAYMENTS = "nexotech.payments";
  public static final String PAYMENTS_EXCHANGE = "nexotech.payments.exchange";
  public static final String PAYMENTS_ROUTING_KEY = "payment.capture";
  public static final String PAYMENTS_DLX = "nexotech.payments.dlx";
  public static final String PAYMENTS_DLQ = "nexotech.payments.dlq";

  private MessagingNames() {
  }

  /** Recursos de la tienda. El administrador no los borra. */
  public static Set<String> storeTopology() {
    return Set.of(
        NOTIFICATIONS,
        NOTIFICATIONS_EXCHANGE,
        NOTIFICATIONS_DLX,
        NOTIFICATIONS_DLQ,
        SHIPMENTS,
        SHIPMENTS_EXCHANGE,
        SHIPMENTS_DLX,
        SHIPMENTS_DLQ,
        PAYMENTS,
        PAYMENTS_EXCHANGE,
        PAYMENTS_DLX,
        PAYMENTS_DLQ);
  }
}
