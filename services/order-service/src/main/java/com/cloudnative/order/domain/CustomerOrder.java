package com.cloudnative.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class CustomerOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(nullable = false)
  private String status;

  /** Suma de las líneas, sin despacho. */
  @Column(nullable = false, precision = 12, scale = 0)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 0)
  private BigDecimal shippingCost = BigDecimal.ZERO;

  /** subtotal + shippingCost. */
  @Column(nullable = false, precision = 12, scale = 0)
  private BigDecimal total;

  @Column(name = "shipping_service")
  private String shippingService;

  @Column(name = "shipping_region")
  private String shippingRegion;

  @Column(name = "shipping_commune")
  private String shippingCommune;

  @Column(name = "payment_id")
  private String paymentId;

  @Column(name = "payment_method")
  private String paymentMethod;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<OrderLine> lines = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public BigDecimal getShippingCost() {
    return shippingCost;
  }

  public void setShippingCost(BigDecimal shippingCost) {
    this.shippingCost = shippingCost;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
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

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<OrderLine> getLines() {
    return lines;
  }

  public void addLine(OrderLine line) {
    line.setOrder(this);
    this.lines.add(line);
  }
}
