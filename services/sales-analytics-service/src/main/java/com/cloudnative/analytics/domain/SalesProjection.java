package com.cloudnative.analytics.domain;

import java.math.BigDecimal;
import java.util.List;
import com.cloudnative.messaging.OrderConfirmed;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesProjection {
  private final JdbcTemplate db;
  public SalesProjection(JdbcTemplate db) { this.db=db; }
  @Transactional
  public synchronized void record(OrderConfirmed event) {
    if (db.queryForObject("SELECT COUNT(*) FROM processed_orders WHERE event_id=?",Integer.class,event.eventId())>0) return;
    db.update("INSERT INTO processed_orders(event_id,order_id,total,created_at) VALUES (?,?,?,?)",
        event.eventId(),event.orderId(),event.total(),java.time.OffsetDateTime.ofInstant(event.createdAt(),java.time.ZoneOffset.UTC));
    for (var item:event.items()) db.update(
        "INSERT INTO order_items(event_id,product_id,quantity,unit_price) VALUES (?,?,?,?)",
        event.eventId(),item.productId(),item.quantity(),item.unitPrice());
  }
  @Transactional(readOnly=true)
  public Summary summary() {
    return new Summary(db.queryForObject("SELECT COUNT(*) FROM processed_orders",Long.class),
        db.queryForObject("SELECT COALESCE(SUM(total),0) FROM processed_orders",BigDecimal.class),
        db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM order_items",Long.class),"CLP");
  }
  public List<ProductSales> products() {
    return db.query("SELECT product_id,SUM(quantity) units FROM order_items GROUP BY product_id ORDER BY units DESC,product_id",
        (rs,row)->new ProductSales(rs.getLong("product_id"),rs.getLong("units")));
  }
  public record Summary(long orderCount,BigDecimal revenue,long units,String currency) {}
  public record ProductSales(long productId,long units) {}
}
