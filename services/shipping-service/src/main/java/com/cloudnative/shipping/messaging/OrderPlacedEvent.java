package com.cloudnative.shipping.messaging;

public class OrderPlacedEvent {

  private String userId;
  private Long orderId;
  private String shippingService;
  private String shippingRegion;
  private String shippingCommune;

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
