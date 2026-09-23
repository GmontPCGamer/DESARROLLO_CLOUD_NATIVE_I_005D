package com.cloudnative.order.messaging;

/**
 * Evento que el productor publica en {@code nexotech.order.notifications}.
 * El consumidor (notification-service) persiste el aviso para el usuario.
 */
public class OrderPlacedMessage {

  private String userId;
  private Long orderId;
  private String title;
  private String message;
  private String type;
  private String paymentId;
  private String shippingService;
  private String shippingRegion;
  private String shippingCommune;

  public OrderPlacedMessage() {
  }

  public OrderPlacedMessage(String userId, Long orderId, String title, String message, String type) {
    this.userId = userId;
    this.orderId = orderId;
    this.title = title;
    this.message = message;
    this.type = type;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getShippingService() {
    return shippingService;
  }

  public void setShippingService(String shippingService) {
    this.shippingService = shippingService;
  }

  public String getShippingRegion() {
    return shippingRegion;
  }

  public void setShippingRegion(String shippingRegion) {
    this.shippingRegion = shippingRegion;
  }

  public String getShippingCommune() {
    return shippingCommune;
  }

  public void setShippingCommune(String shippingCommune) {
    this.shippingCommune = shippingCommune;
  }
}
