package com.cloudnative.rabbitadmin.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import com.cloudnative.messaging.MessagingNames;
import com.cloudnative.rabbitadmin.web.QueueRequest;

class RabbitAdminServiceTest {

  private final RabbitAdmin admin = mock(RabbitAdmin.class);
  private final RabbitAdminService service = new RabbitAdminService(admin);

  @Test
  void rechazaNombreVacio() {
    assertThrows(IllegalArgumentException.class, () -> service.deleteQueue("  "));
  }

  @Test
  void noBorraLaTopologiaDeLaTienda() {
    assertThrows(IllegalArgumentException.class, () -> service.deleteQueue(MessagingNames.NOTIFICATIONS));
  }

  @Test
  void borraUnaColaDeDemostracion() {
    service.deleteQueue("demo.cola");
    verify(admin).deleteQueue("demo.cola");
  }

  @Test
  void creaColaDurable() {
    service.createQueue(new QueueRequest("demo.cola", true, null, null));
    verify(admin).declareQueue(org.mockito.ArgumentMatchers.any());
  }
}
