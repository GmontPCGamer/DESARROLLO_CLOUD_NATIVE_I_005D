CREATE TABLE IF NOT EXISTS processed_orders (
  event_id VARCHAR(100) PRIMARY KEY,
  order_id BIGINT NOT NULL,
  total DECIMAL(19,2) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS order_items (
  event_id VARCHAR(100) NOT NULL REFERENCES processed_orders(event_id),
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL,
  unit_price DECIMAL(19,2) NOT NULL,
  PRIMARY KEY (event_id, product_id)
);
