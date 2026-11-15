CREATE TABLE IF NOT EXISTS offer_reviews (
  event_id VARCHAR(255) PRIMARY KEY,
  product_id BIGINT NOT NULL,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX IF NOT EXISTS offer_reviews_product_idx ON offer_reviews(product_id);
