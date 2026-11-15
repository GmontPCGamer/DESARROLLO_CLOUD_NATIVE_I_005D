# NexoTech

Tienda de tecnología desarrollada para **Desarrollo Cloud Native I (005D)**. Incluye catálogo público, inventario, carrito, compras con pago simulado, cotización de despacho, reseñas, notificaciones y administración. RabbitMQ mueve las tareas operativas de la tienda y Kafka procesa eventos para ofertas y análisis de ventas.

## Acceso rápido para la presentación

| Abre | Enlace | Acceso |
| --- | --- | --- |
| Tienda NexoTech en AWS | [Abrir frontend](https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/web/) | Público |
| Panel de eventos y ventas | [Abrir Administración](https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/web/admin) | Cuenta Duoc con rol administrador |
| Kafka UI en AWS | [Abrir Kafka UI](http://127.0.0.1:18093/dev/kafka/) | Túnel de AWS y credenciales de la demo |
| RabbitMQ Management en AWS | [Abrir RabbitMQ](http://127.0.0.1:18072/) | Túnel de AWS y credenciales de la demo |

Antes de presentar, ejecuta este comando desde la raíz del repositorio **en el mismo equipo donde vas a abrir los dashboards**:

```bash
bash scripts/start-monitoring-tunnels.sh
```

Usa AWS CLI con una sesión de AWS Academy vigente y el complemento Session Manager. Espera a que aparezcan los puertos `18093` y `18072` y deja la terminal abierta. Los enlaces `127.0.0.1` conectan ese navegador a los dashboards que corren en EC2. Para compartir la demo con el público, usa el enlace HTTPS del frontend.

- [Comprobar salud de la API](https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api/public/health)
- [Ver ofertas generadas a partir de reseñas](https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api/public/offers)
- [Ver cambios de la integración EP3](https://github.com/GmontPCGamer/DESARROLLO_CLOUD_NATIVE_I_005D/pull/1)

## Arquitectura

La interfaz Angular 22 se sirve desde nginx en EC2. AWS API Gateway HTTP v2 proporciona HTTPS y enruta `/dev/web/` hacia la interfaz y `/dev/api/` hacia el backend. El backend actúa como fachada (BFF) para 12 microservicios Spring Boot 4. El catálogo y las ofertas se pueden consultar sin sesión. Las compras, reseñas nuevas y notificaciones requieren una cuenta autorizada en el tenant Duoc de Microsoft Entra ID.

Angular usa MSAL con el flujo de código de autorización y PKCE. Envía el token Bearer al invocar rutas protegidas. API Gateway valida el JWT y el BFF verifica el acceso a la API de la aplicación. Las rutas de administración requieren `ROLE_ADMIN`; las cuentas autorizadas se configuran mediante `APP_ADMIN_USERS`. La aplicación desplegada pide el scope `api://nexotech-demo-api/access_as_user` y vuelve a [la ruta de autenticación](https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/web/auth).

| Proceso | Puerto | Responsabilidad |
| --- | ---: | --- |
| `backend` | 8080 | BFF, seguridad y llamadas a los microservicios |
| `catalog-service` | 8081 | Catálogo y stock visible |
| `cart-service` | 8082 | Carrito por usuario |
| `order-service` | 8083 | Confirmación de pedidos y publicación en RabbitMQ y Kafka |
| `notification-service` | 8084 | Consumo y consulta de avisos |
| `inventory-service` | 8085 | Reserva y reposición de inventario |
| `payment-service` | 8086 | Autorización y captura de pagos simulados |
| `shipping-service` | 8087 | Cotización y registro de despachos |
| `review-service` | 8088 | Reseñas y publicación en RabbitMQ y Kafka |
| `rabbit-admin-service` | 8090 | Consulta y gestión de colas, exchanges y bindings por el BFF |
| `offer-service` | 8091 | Proyección de ofertas a partir de reseñas |
| `sales-analytics-service` | 8092 | Proyección persistente de ventas y unidades |
| `kafka-admin-service` | 8094 | Gestión de topics e inspección de grupos de consumidores |

## Mensajería

### RabbitMQ

Tras confirmar un pedido, se publican mensajes para los tres flujos operativos. Las reseñas y los despachos también generan avisos. Las colas tienen exchanges directos y rutas de mensajes fallidos (DLX y DLQ). Los consumidores confirman el procesamiento con ACK.

| Cola | Routing key | Resultado |
| --- | --- | --- |
| `nexotech.notifications` | `notification.user` | Aviso para el usuario |
| `nexotech.payments` | `payment.capture` | Captura de pago simulado |
| `nexotech.shipments` | `shipment.create` | Despacho registrado |

### Kafka

El clúster usa ZooKeeper y dos brokers. Cada topic de negocio tiene tres particiones y dos réplicas en el despliegue. Los consumidores reintentan fallos y derivan mensajes inválidos a los topics `.DLT` correspondientes.

| Topic | Productor | Consumidor | Entrada y salida |
| --- | --- | --- | --- |
| `nexotech.review-created` | `review-service` | `offer-service` | Evento con ID, producto y valoración → producto destacado con conteo de reseñas en `/api/public/offers` |
| `nexotech.order-confirmed` | `order-service` | `sales-analytics-service` | Evento con ID, pedido, total e ítems → compras procesadas, ventas y unidades en `/api/admin/analytics/sales` |

Los [contratos JSON](services/messaging-contract/src/main/resources/schemas/) incluyen los eventos de entrada y las respuestas de ofertas y analítica. El módulo [messaging-contract](services/messaging-contract/) también contiene los nombres de la topología de RabbitMQ y los tipos de evento de Kafka.

Ofertas persiste la proyección y los identificadores de eventos con el grupo estable `offer-service-v2`. La primera migración lee el historial desde el inicio. Las réplicas de ofertas deben compartir la misma base PostgreSQL mediante `OFFER_DB_URL`, `OFFER_DB_USER` y `OFFER_DB_PASSWORD`. En AWS, ofertas usa PostgreSQL 17 en un contenedor separado en la EC2 de aplicación. La base tiene un volumen persistente y escucha únicamente en el puerto local `15432`. El instalador genera las credenciales en la instancia y las carga desde un archivo privado de systemd. En local, el valor predeterminado es H2 en disco. Analítica usa el grupo estable `sales-analytics-v1` y guarda la proyección y los identificadores procesados en H2 en disco para mantener los totales y evitar duplicados tras reinicios. El panel **Administración → Eventos y ventas** muestra la analítica, las particiones y réplicas, la configuración de topics y los grupos de consumidores. Permite probar creación, cambios de retención o particiones y eliminación con topics `demo.*`.

## Ejecución local

Requisitos recomendados para esta versión: Java 17, Maven, Node.js 24 compatible con Angular 22, npm, Python 3 y Docker con Compose.

```bash
bash scripts/start-all.sh local
python3 scripts/smoke-messaging.py --all-flows --restart-offers --restart-analytics
```

El script levanta ambos brokers Kafka, ZooKeeper, Kafka UI y los dos nodos RabbitMQ, instala el contrato compartido, compila con pruebas y arranca los 13 procesos. El perfil `local` acepta JWT de desarrollo y el script de prueba genera su propio token. Para iniciar sesión desde Angular con Microsoft, usa `bash scripts/start-all.sh azuread` con una cuenta Duoc autorizada y el retorno de desarrollo registrado en Entra.

En otra terminal:

```bash
cd frontend
npm ci
npm start
```

- [Frontend local](http://localhost:4200/)
- [BFF local](http://localhost:8080/api/public/health)
- [Kafka UI local](http://localhost:8093/dev/kafka/)
- [RabbitMQ Management local](http://localhost:15672/)

Docker Compose define los usuarios de desarrollo de los dashboards y sus variables de entorno. Mantén las credenciales de AWS, Entra y los brokers de la nube en sus mecanismos de configuración privados. Los logs locales quedan en `.run/logs/`.

```bash
bash scripts/stop-all.sh
docker compose down
```

Para ejecutar las pruebas Maven por separado:

```bash
mvn -q -f services/messaging-contract/pom.xml install
for service in review offer order sales-analytics kafka-admin; do
  mvn -q -f "services/${service}-service/pom.xml" test || exit 1
done
```

## Despliegue en AWS Academy

La demo corre en `us-east-1`. La EC2 de aplicación es una `t3.large` de 8 GB con nginx, 13 procesos administrados por systemd y el clúster Kafka con Kafka UI en Docker. RabbitMQ corre en una EC2 separada con dos nodos. La configuración de infraestructura, incluyendo el API Gateway, el stage `dev` y la Elastic IP, está en [terraform/](terraform/).

Para actualizar una instalación existente, genera los JAR con Maven y compila el frontend con `npm ci` y `npm run build` desde `frontend/`. Luego ejecuta, desde la raíz y con una sesión AWS válida:

```bash
INSTANCE_ID='id-de-la-ec2' ARTIFACT_BUCKET='nombre-del-bucket-privado' bash scripts/deploy-ssm.sh
```

El script envía el paquete al bucket privado y entrega un ID de comando de Systems Manager para consultar el resultado. La instalación conserva las releases anteriores y la configuración de la instancia. `scripts/configure-offers-postgres.sh` prepara PostgreSQL antes de iniciar ofertas. Para actualizar solo la interfaz, usa `bash scripts/deploy-frontend.sh`. Si cambia la infraestructura pública, `scripts/configure-public-cloud.py` actualiza rutas, CORS y el retorno de Entra con AWS CLI y Azure CLI.

### Estabilidad del enlace público

El enlace HTTPS **se mantiene al detener, iniciar o reiniciar la EC2**. El dominio lo identifica el ID de API Gateway `ourd5f7qr1`, y la ruta usa el stage `dev` y `/web/`. Durante el apagado o mientras se levantan los servicios, la misma dirección puede quedar temporalmente inaccesible. Una API nueva tendría otro ID. Cambiar el nombre del stage o la ruta también cambia el enlace que se comparte. [Stages de HTTP API en AWS](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-stages.html)

API Gateway llega a nginx y al BFF mediante la **Elastic IP** de la EC2. Esa IP es estática mientras siga asignada a la cuenta. Si se reemplaza la instancia, se puede asociar la misma Elastic IP a la nueva EC2 y mantener el enlace público, siempre que se conserven la API, el stage y las rutas. Si se usa otra IP, hay que actualizar las integraciones de API Gateway. [Elastic IP en AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html)

### Monitoreo y comprobaciones

Consulta [Acceso rápido para la presentación](#acceso-rápido-para-la-presentación) para abrir Kafka UI y RabbitMQ Management en AWS.

```bash
BASE=https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev
curl -fsS "$BASE/web/" >/dev/null
curl -fsS "$BASE/api/public/health"
curl -fsS "$BASE/api/public/offers"
curl -sS -o /dev/null -w '%{http_code}\n' "$BASE/api/me" # 401 sin token
```

Las pruebas cubren publicación y consumo, mensajes inválidos hacia DLT, reparto de particiones entre dos consumidores, deduplicación, persistencia y reenvío tras fallos del broker. En la demo se comprobaron el login Entra, CORS, una reseña, una compra simulada, la actualización del panel de ventas y los ACK de RabbitMQ para pago y despacho. Kafka UI mostró los dos brokers y sus métricas.

La demo conserva parte del estado operativo en memoria. Reiniciar los servicios puede reiniciar carritos, pedidos o reseñas, según el servicio. Ofertas y analítica conservan sus proyecciones en disco. Pedidos y reseñas guardan los eventos pendientes en disco y reintentan su envío a Kafka hasta recibir confirmación. El registro de la operación de negocio y su encolado siguen siendo escrituras separadas. Los consumidores deduplican por ID para tolerar reenvíos. RabbitMQ mantiene su propio flujo de publicación.

## Estructura del repositorio

- [frontend/](frontend/) contiene la SPA Angular.
- [backend/](backend/) contiene el BFF Spring Boot.
- [services/](services/) contiene los microservicios y el contrato de mensajería.
- [docker-compose.yml](docker-compose.yml) define RabbitMQ, ZooKeeper, Kafka y Kafka UI para desarrollo.
- [rabbitmq/](rabbitmq/) contiene la configuración del clúster RabbitMQ.
- [scripts/](scripts/) contiene el arranque, despliegue y pruebas de integración.
- [terraform/](terraform/) contiene la infraestructura AWS.

### Operaciones sobre grupos Kafka

```bash
bash scripts/kafka-consumer-groups.sh list
bash scripts/kafka-consumer-groups.sh inspect offer-service-v2
bash scripts/kafka-consumer-groups.sh preview-reset demo.consumidor demo.eventos
bash scripts/kafka-consumer-groups.sh reset demo.consumidor demo.eventos --confirm
```

Para ensayar el reinicio, crea antes el topic de prueba y consume al menos un mensaje con ese grupo. Detén el consumidor antes de aplicar el cambio. `inspect` muestra offsets y lag. En EC2, `KAFKA_COMPOSE_FILE` puede indicar la ruta al Compose de la instalación.

En un entorno local de prueba, usa `KAFKA_UI_READONLY=false docker compose up -d kafka-ui` para demostrar la administración desde Kafka UI.
