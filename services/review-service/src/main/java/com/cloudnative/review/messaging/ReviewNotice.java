package com.cloudnative.review.messaging;

public class ReviewNotice {

  private String userId;
  private String title;
  private String message;
  private String type;

  public ReviewNotice() {
  }

  public ReviewNotice(String userId, String title, String message, String type) {
    this.userId = userId;
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
