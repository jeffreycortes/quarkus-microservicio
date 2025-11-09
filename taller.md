# HORA 3: DOCKER Y BASE DE DATOS
## 3.1 Dockerfile Multi-stage
dockerfile
```
# Dockerfile
FROM quay.io/quarkus/centos-quarkus-maven:3.2.0.Final-java17 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /usr/src/app/target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```
# 3.2 Flyway (opcional)
Habilitar inicialización de BD automático
```
quarkus.flyway.migrate-at-start=true
```

# HORA 5: COMUNICACIÓN ASÍNCRONA CON KAFKA
# 5.1 Configuración Kafka en application.properties
```
#properties

# Kafka
kafka.bootstrap.servers=localhost:9092
mp.messaging.incoming.orders-in.connector=smallrye-kafka
mp.messaging.incoming.orders-in.topic=orders
mp.messaging.incoming.orders-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer

mp.messaging.outgoing.orders-out.connector=smallrye-kafka
mp.messaging.outgoing.orders-out.topic=processed-orders
mp.messaging.outgoing.orders-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer
```
# 5.2 Producer y Consumer con Reactive Messaging
```
// Order Producer
@ApplicationScoped
public class OrderProducer {
    
    @Channel("orders-out")
    Emitter<String> orderEmitter;
    
    public void sendOrder(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            orderEmitter.send(orderJson);
            System.out.println("✅ Orden enviada: " + order.id());
        } catch (Exception e) {
            System.err.println("❌ Error enviando orden: " + e.getMessage());
        }
    }
}

// Order Consumer
@ApplicationScoped
public class OrderConsumer {
    
    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);
    
    @Incoming("orders-in")
    public void processOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);
            LOG.infof("📦 Procesando orden: %s - %s", order.id(), order.productName());
            
            // Simular procesamiento
            processOrderAsync(order);
            
        } catch (Exception e) {
            LOG.error("Error procesando orden: " + e.getMessage());
        }
    }
    
    private void processOrderAsync(Order order) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000); // Simular procesamiento
                LOG.infof("✅ Orden completada: %s", order.id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}

// Record para Orders
public record Order(String id, String productName, Integer quantity, String customerEmail) {}
```

# 5.3 Resource para Enviar Órdenes
```
@Path("/orders")
public class OrderResource {

    @Inject
    OrderProducer orderProducer;
    
    @POST
    public Response createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + System.currentTimeMillis();
        Order order = new Order(orderId, request.productName(), request.quantity(), request.customerEmail());
        
        orderProducer.sendOrder(order);
        
        return Response.accepted()
            .entity(Map.of(
                "orderId", orderId,
                "status", "PROCESSING",
                "message", "Orden recibida y en procesamiento"
            ))
            .build();
    }
}
```

# 5.4 Docker Compose con Kafka
```
# docker-compose.kafka.yml
version: '3.8'
services:
zookeeper:
image: confluentinc/cp-zookeeper:latest
environment:
ZOOKEEPER_CLIENT_PORT: 2181

kafka:
image: confluentinc/cp-kafka:latest
depends_on:
- zookeeper
ports:
- "9092:9092"
environment:
KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

# HORA 6: PATRÓN SAGA Y DESCUBRIMIENTO
## 6.1 Implementación Patrón SAGA (Orchestration)
```
// Saga Orchestrator
@ApplicationScoped
public class OrderSagaOrchestrator {
    
    @Inject
    OrderService orderService;
    
    @Inject
    InventoryService inventoryService;
    
    @Inject
    PaymentService paymentService;
    
    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);
    
    public void processOrderSaga(OrderSaga order) {
        LOG.infof("🚀 Iniciando SAGA para orden: %s", order.getOrderId());
        
        try {
            // Paso 1: Reservar inventario
            boolean inventoryReserved = inventoryService.reserveInventory(
                order.getProductId(), order.getQuantity());
            
            if (!inventoryReserved) {
                orderService.updateStatus(order.getOrderId(), "FAILED", "Inventario insuficiente");
                return;
            }
            
            // Paso 2: Procesar pago
            boolean paymentProcessed = paymentService.processPayment(
                order.getOrderId(), order.getAmount(), order.getCustomerId());
            
            if (!paymentProcessed) {
                // Compensación: Liberar inventario
                inventoryService.releaseInventory(order.getProductId(), order.getQuantity());
                orderService.updateStatus(order.getOrderId(), "FAILED", "Pago rechazado");
                return;
            }
            
            // Paso 3: Confirmar orden
            orderService.updateStatus(order.getOrderId(), "COMPLETED", "Orden completada exitosamente");
            LOG.infof("✅ SAGA completada exitosamente para orden: %s", order.getOrderId());
            
        } catch (Exception e) {
            LOG.error("❌ Error en SAGA: " + e.getMessage());
            orderService.updateStatus(order.getOrderId(), "FAILED", "Error en el proceso: " + e.getMessage());
        }
    }
}

// Entidad SAGA
@Entity
@Table(name = "order_sagas")
public class OrderSaga extends PanacheEntity {
    
    @Column(name = "order_id", unique = true)
    public String orderId;
    
    @Column(name = "customer_id")
    public String customerId;
    
    @Column(name = "product_id")
    public String productId;
    
    public Integer quantity;
    
    public Double amount;
    
    @Enumerated(EnumType.STRING)
    public SagaStatus status = SagaStatus.PENDING;
    
    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
    
    public enum SagaStatus {
        PENDING, COMPLETED, FAILED, COMPENSATING
    }
}
```

# 6.2 Service Discovery con MicroProfile Rest Client
```
// Inventory Service Client
@RegisterRestClient(baseUri = "http://inventory-service:8080")
@Path("/inventory")
public interface InventoryService {
    
    @POST
    @Path("/reserve")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    boolean reserveInventory(ReserveRequest request);
    
    @POST
    @Path("/release")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    boolean releaseInventory(ReleaseRequest request);
}

// Payment Service Client  
@RegisterRestClient(baseUri = "http://payment-service:8080")
@Path("/payments")
public interface PaymentService {
    
    @POST
    @Path("/process")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    boolean processPayment(PaymentRequest request);
}

// Record para requests
public record ReserveRequest(String productId, Integer quantity) {}
public record ReleaseRequest(String productId, Integer quantity) {}
public record PaymentRequest(String orderId, Double amount, String customerId) {}
```

# 6.3 Implementación de los Servicios
```
// Inventory Service Implementation
@ApplicationScoped
public class InventoryServiceImpl implements InventoryService {

    @Override
    public boolean reserveInventory(ReserveRequest request) {
        // Lógica de reserva de inventario
        System.out.printf("📦 Reservando %d unidades del producto %s%n", 
            request.quantity(), request.productId());
        return true; // Simulación
    }
    
    @Override
    public boolean releaseInventory(ReleaseRequest request) {
        // Lógica de liberación de inventario
        System.out.printf("🔄 Liberando %d unidades del producto %s%n", 
            request.quantity(), request.productId());
        return true;
    }
}

// Payment Service Implementation
@ApplicationScoped  
public class PaymentServiceImpl implements PaymentService {

    @Override
    public boolean processPayment(PaymentRequest request) {
        // Lógica de procesamiento de pago
        System.out.printf("💳 Procesando pago de $%.2f para orden %s%n", 
            request.amount(), request.orderId());
        return Math.random() > 0.2; // 80% de éxito simulado
    }
}
```

# HORA 7: API GATEWAY Y KUBERNETES
## 7.1 API Gateway con Quarkus
```
// Gateway Resource
@Path("/gateway")
public class ApiGatewayResource {
    
    @RestClient
    InventoryService inventoryService;
    
    @RestClient
    PaymentService paymentService;
    
    @GET
    @Path("/inventory/{productId}")
    public Response getInventory(@PathParam("productId") String productId) {
        try {
            // Lógica de gateway para inventory
            return Response.ok(Map.of("productId", productId, "stock", 100)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/orders")
    @Transactional
    public Response createOrderThroughGateway(CreateOrderRequest request) {
        try {
            // 1. Verificar inventario
            boolean available = inventoryService.reserveInventory(
                new ReserveRequest(request.productId(), request.quantity()));
            
            if (!available) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Producto no disponible"))
                    .build();
            }
            
            // 2. Procesar pago
            String orderId = "ORD-" + System.currentTimeMillis();
            boolean paymentSuccess = paymentService.processPayment(
                new PaymentRequest(orderId, request.amount(), request.customerId()));
            
            if (!paymentSuccess) {
                // Compensación
                inventoryService.releaseInventory(
                    new ReleaseRequest(request.productId(), request.quantity()));
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Pago rechazado"))
                    .build();
            }
            
            return Response.ok(Map.of(
                "orderId", orderId,
                "status", "CONFIRMED",
                "message", "Orden creada exitosamente"
            )).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}
```

# 7.2 Configuración Kubernetes con Quarkus
## 7.2 Configuración Kubernetes con Quarkus
```
# src/main/resources/application.properties
quarkus.kubernetes.deploy=true
quarkus.kubernetes.client.trust-certs=true
quarkus.kubernetes.deployment-target=kubernetes

# Service configuration
quarkus.kubernetes.service-type=ClusterIP
quarkus.kubernetes.replicas=2

# Resource limits
quarkus.kubernetes.resources.limits.memory=512Mi
quarkus.kubernetes.resources.limits.cpu=500m
quarkus.kubernetes.resources.requests.memory=256Mi
quarkus.kubernetes.resources.requests.cpu=100m

# Health checks
quarkus.kubernetes.liveness-probe.http-action-path=/q/health/live
quarkus.kubernetes.readiness-probe.http-action-path=/q/health/ready
```

## 7.3 Kubernetes Deployment Manifests (generados automáticamente)
```
# Generar recursos Kubernetes
./mvnw clean package -Dquarkus.kubernetes.deploy=true

# Los archivos se generan en target/kubernetes/
ls target/kubernetes/
# kubernetes.json  kubernetes.yml
```

### Ejemplo de deployment.yml generado:
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-galaxia
  labels:
    app: hello-galaxia
    version: 1.0.0
spec:
  replicas: 2
  selector:
    matchLabels:
      app: hello-galaxia
  template:
    metadata:
      labels:
        app: hello-galaxia
    spec:
      containers:
      - name: hello-galaxia
        image: galaxia/hello:1.0.0
        ports:
        - containerPort: 8080
          protocol: TCP
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
          requests:
            memory: "256Mi"
            cpu: "100m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 5
```

# HORA 8: SEGURIDAD JWT Y RBAC
## 8.1 Configuración JWT Security
```
# JWT Configuration
mp.jwt.verify.publickey.location=META-INF/resources/publicKey.pem
mp.jwt.verify.issuer=https://galaxia.example.com

# Security
quarkus.security.jwt.enabled=true
quarkus.http.auth.permission.authenticated.paths=/*
quarkus.http.auth.permission.authenticated.policy=authenticated

# Roles paths
quarkus.http.auth.permission.admin.paths=/admin/*
quarkus.http.auth.permission.admin.policy=role=admin

quarkus.http.auth.permission.user.paths=/api/*
quarkus.http.auth.permission.user.policy=role=user
```

## 8.2 Resource con Seguridad por Roles
```
@Path("/secure")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecureResource {
    
    @Inject
    JsonWebToken jwt;
    
    @GET
    @Path("/public")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "Este endpoint es público");
    }
    
    @GET
    @Path("/user")
    @RolesAllowed("user")
    public Map<String, Object> userEndpoint() {
        return Map.of(
            "message", "Hola usuario",
            "username", jwt.getName(),
            "groups", jwt.getGroups()
        );
    }
    
    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public Map<String, Object> adminEndpoint() {
        return Map.of(
            "message", "Hola administrador",
            "username", jwt.getName(),
            "isAdmin", true
        );
    }
    
    @POST
    @Path("/products")
    @RolesAllowed("admin")
    @Transactional
    public Response createProduct(@Valid ProductEntity product) {
        productRepository.persist(product);
        return Response.status(Response.Status.CREATED).entity(product).build();
    }
}
```

## 8.3 Generador de Tokens JWT para Testing
```
@ApplicationScoped
public class TokenService {
    
    public String generateUserToken(String username) {
        return generateToken(username, Set.of("user"));
    }
    
    public String generateAdminToken(String username) {
        return generateToken(username, Set.of("admin", "user"));
    }
    
    private String generateToken(String username, Set<String> groups) {
        try {
            return Jwt.issuer("https://galaxia.example.com")
                .upn(username)
                .groups(groups)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign();
        } catch (Exception e) {
            throw new RuntimeException("Error generando token", e);
        }
    }
}

// Endpoint para obtener tokens (solo desarrollo)
@Path("/auth")
public class AuthResource {
    
    @Inject
    TokenService tokenService;
    
    @POST
    @Path("/token/user")
    public Map<String, String> getUserToken(@QueryParam("username") String username) {
        String token = tokenService.generateUserToken(username);
        return Map.of("token", token);
    }
    
    @POST
    @Path("/token/admin") 
    public Map<String, String> getAdminToken(@QueryParam("username") String username) {
        String token = tokenService.generateAdminToken(username);
        return Map.of("token", token);
    }
}
```

# HORA 9: RESILIENCIA Y OBSERVABILIDAD
## 9.1 Fault Tolerance con MicroProfile
```
@ApplicationScoped
public class ResilientService {
    
    private static final Logger LOG = Logger.getLogger(ResilientService.class);
    
    private int attemptCount = 0;
    
    @Timeout(5000) // Timeout de 5 segundos
    @Fallback(fallbackMethod = "fallbackOperation")
    public String performOperation() {
        attemptCount++;
        LOG.infof("Intento de operación: %d", attemptCount);
        
        // Simular fallo aleatorio
        if (Math.random() < 0.3) {
            throw new RuntimeException("Error simulado en la operación");
        }
        
        // Simular procesamiento largo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return "Operación completada exitosamente en intento " + attemptCount;
    }
    
    @Retry(maxRetries = 3, delay = 1000)
    @CircuitBreaker(
        requestVolumeThreshold = 4,
        failureRatio = 0.5,
        delay = 10000,
        successThreshold = 2
    )
    @Bulkhead(value = 5) // Máximo 5 llamadas concurrentes
    public String reliableOperation() {
        attemptCount++;
        
        // Simular diferentes tipos de fallos
        double random = Math.random();
        if (random < 0.2) {
            throw new RuntimeException("Error temporal");
        } else if (random < 0.4) {
            try {
                Thread.sleep(3000); // Timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return "Operación confiable completada";
    }
    
    // Fallback method
    public String fallbackOperation() {
        LOG.warn("Usando fallback para operación");
        return "Operación fallback - servicio temporalmente no disponible";
    }
}
```

## 9.2 Health Checks Personalizados
```
@Liveness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    
    @Inject
    ProductRepository productRepository;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Verificar conexión a base de datos
            productRepository.count();
            return HealthCheckResponse.up("Database connection");
        } catch (Exception e) {
            return HealthCheckResponse.down("Database connection");
        }
    }
}

@Readiness
@ApplicationScoped  
public class ServiceHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResponse call() {
        // Verificar dependencias externas
        boolean kafkaHealthy = checkKafka();
        boolean inventoryServiceHealthy = checkInventoryService();
        
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("External services");
        
        if (kafkaHealthy && inventoryServiceHealthy) {
            return builder.up().build();
        } else {
            return builder.down()
                .withData("kafka", kafkaHealthy ? "UP" : "DOWN")
                .withData("inventory", inventoryServiceHealthy ? "UP" : "DOWN")
                .build();
        }
    }
    
    private boolean checkKafka() {
        // Lógica de verificación de Kafka
        return true;
    }
    
    private boolean checkInventoryService() {
        // Lógica de verificación de Inventory Service
        return true;
    }
}
```

## 9.3 Métricas y Métricas Personalizadas
```
@ApplicationScoped
public class OrderMetrics {
    
    @Inject
    MeterRegistry meterRegistry;
    
    private final Counter ordersCreated;
    private final Counter ordersFailed;
    private final Timer orderProcessingTimer;
    
    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total de órdenes creadas")
            .register(meterRegistry);
            
        this.ordersFailed = Counter.builder("orders.failed")
            .description("Total de órdenes fallidas")
            .register(meterRegistry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Tiempo de procesamiento de órdenes")
            .register(meterRegistry);
    }
    
    public void recordOrderCreated() {
        ordersCreated.increment();
    }
    
    public void recordOrderFailed() {
        ordersFailed.increment();
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopProcessingTimer(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
    }
}

// Uso en el servicio
@ApplicationScoped
public class OrderService {
    
    @Inject
    OrderMetrics metrics;
    
    public void processOrder(Order order) {
        Timer.Sample timer = metrics.startProcessingTimer();
        
        try {
            // Procesar orden
            processOrderLogic(order);
            metrics.recordOrderCreated();
            
        } catch (Exception e) {
            metrics.recordOrderFailed();
            throw e;
        } finally {
            metrics.stopProcessingTimer(timer);
        }
    }
}
```

# HORA 10: PATRONES AVANZADOS
## 10.1 Event Sourcing Básico
```
// Event
public abstract class DomainEvent {
    public final String aggregateId;
    public final Instant timestamp;
    
    public DomainEvent(String aggregateId) {
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
    }
}

// Eventos específicos
public class OrderCreatedEvent extends DomainEvent {
    public final String orderId;
    public final String customerId;
    public final Double amount;
    
    public OrderCreatedEvent(String orderId, String customerId, Double amount) {
        super(orderId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
    }
}

public class OrderPaidEvent extends DomainEvent {
    public final String orderId;
    public final String paymentId;
    
    public OrderPaidEvent(String orderId, String paymentId) {
        super(orderId);
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
}

// Event Store
@ApplicationScoped
public class EventStore {
    
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();
    
    public void append(DomainEvent event) {
        events.add(event);
        System.out.printf("📝 Evento almacenado: %s - %s%n", 
            event.getClass().getSimpleName(), event.aggregateId);
    }
    
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        return events.stream()
            .filter(event -> event.aggregateId.equals(aggregateId))
            .collect(Collectors.toList());
    }
    
    public List<DomainEvent> getAllEvents() {
        return new ArrayList<>(events);
    }
}
```

## 10.2 CQRS - Separación Lectura/Escritura
```
// Command Side (Escritura)
@ApplicationScoped
public class OrderCommandService {
    
    @Inject
    EventStore eventStore;
    
    @Transactional
    public String createOrder(CreateOrderCommand command) {
        String orderId = "ORD-" + System.currentTimeMillis();
        
        // Publicar evento
        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId, command.customerId(), command.amount());
        eventStore.append(event);
        
        return orderId;
    }
    
    @Transactional
    public void markOrderAsPaid(MarkOrderPaidCommand command) {
        OrderPaidEvent event = new OrderPaidEvent(
            command.orderId(), command.paymentId());
        eventStore.append(event);
    }
}

// Query Side (Lectura)
@ApplicationScoped
public class OrderQueryService {
    
    @Inject
    EventStore eventStore;
    
    public OrderProjection getOrder(String orderId) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(orderId);
        return rebuildProjection(events);
    }
    
    private OrderProjection rebuildProjection(List<DomainEvent> events) {
        OrderProjection projection = new OrderProjection();
        
        for (DomainEvent event : events) {
            if (event instanceof OrderCreatedEvent created) {
                projection.apply(created);
            } else if (event instanceof OrderPaidEvent paid) {
                projection.apply(paid);
            }
        }
        
        return projection;
    }
}

// Proyección
public class OrderProjection {
    public String orderId;
    public String customerId;
    public Double amount;
    public boolean paid = false;
    public String paymentId;
    public OrderStatus status = OrderStatus.CREATED;
    
    public void apply(OrderCreatedEvent event) {
        this.orderId = event.orderId;
        this.customerId = event.customerId;
        this.amount = event.amount;
    }
    
    public void apply(OrderPaidEvent event) {
        this.paid = true;
        this.paymentId = event.paymentId;
        this.status = OrderStatus.PAID;
    }
    
    public enum OrderStatus {
        CREATED, PAID, SHIPPED, DELIVERED
    }
}
```