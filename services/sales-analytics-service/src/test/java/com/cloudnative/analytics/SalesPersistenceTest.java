package com.cloudnative.analytics;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.nio.file.Path;
import com.cloudnative.analytics.domain.SalesProjection;
import com.cloudnative.messaging.OrderConfirmed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.*;
class SalesPersistenceTest {
 @TempDir Path directory;
 @Test void rebuildingServiceRetainsProjectionAndDeduplication(){
  String url="jdbc:h2:file:"+directory.resolve("sales");
  var ds=new DriverManagerDataSource(url,"sa","");
  new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
  var event=new OrderConfirmed("persistent",1L,BigDecimal.TEN,"CLP",Instant.now(),List.of(new OrderConfirmed.Item(1L,1,BigDecimal.TEN)));
  new SalesProjection(new JdbcTemplate(ds)).record(event);
  var restarted=new SalesProjection(new JdbcTemplate(new DriverManagerDataSource(url,"sa","")));
  restarted.record(event);
  assertThat(restarted.summary().orderCount()).isEqualTo(1);
  assertThat(restarted.summary().revenue()).isEqualByComparingTo("10");
 }
}
