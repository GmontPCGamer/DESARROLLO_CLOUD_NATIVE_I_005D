package com.cloudnative.shipping.messaging;

public class ShipmentNotice {

  private String userId;
  private Long orderId;
  private String title;
  private String message;
  private String type;

  public ShipmentNotice() {
  }

  public ShipmentNotice(String userId, Long orderId, String title, String message, String type) {
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
}
