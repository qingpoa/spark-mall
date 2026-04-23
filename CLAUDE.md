# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, test, and run commands

This is a multi-module Maven project rooted at `sparkle-shop-cloud`.

- Build all modules:
  - `mvn clean package`
- Run all tests:
  - `mvn test`
- Run one module's tests and build required upstream modules:
  - `mvn -pl sparkle-services/user-service -am test`
- Run a single test class:
  - `mvn -pl sparkle-services/user-service -am -Dtest=UserAuthServiceImplTest test`
- Run a single test method:
  - `mvn -pl sparkle-services/user-service -am -Dtest=UserAuthServiceImplTest#shouldRegisterAndReturnJwt test`
- Start local infrastructure declared in `docker/docker-compose.yml`:
  - `docker compose -f docker/docker-compose.yml up -d`
- Run a service locally:
  - `mvn -pl sparkle-services/user-service -am spring-boot:run`
  - `mvn -pl sparkle-services/product-service -am spring-boot:run`
  - `mvn -pl sparkle-services/cart-service -am spring-boot:run`
  - `mvn -pl sparkle-services/order-service -am spring-boot:run`
  - `mvn -pl sparkle-services/stock-service -am spring-boot:run`
  - `mvn -pl sparkle-services/coupon-service -am spring-boot:run`
  - `mvn -pl sparkle-gateway/gateway-service -am spring-boot:run`

## High-level architecture

### Module layout

The root `pom.xml` is an aggregator with three top-level module groups:

- `sparkle-common`: shared libraries used by all services
- `sparkle-gateway`: the API gateway
- `sparkle-services`: business microservices (`user-service`, `product-service`, `cart-service`, `order-service`, `stock-service`, `coupon-service`)

This is a Spring Boot 3 / Spring Cloud project on Java 17 with Maven dependency management centralized in the root `pom.xml`.

### Shared foundation in `sparkle-common`

The common modules define the cross-cutting runtime behavior that most code relies on:

- `common-core`
  - shared response and exception primitives such as `Result`, `PageResponse`, and `BusinessException`
- `common-web`
  - HTTP response helpers and global exception translation via `Results` and `GlobalExceptionHandler`
- `common-security`
  - JWT-based authentication support, `@RequireLogin`, `JwtAuthenticationInterceptor`, token models/services, and security auto-configuration
- `common-log`
  - request trace propagation via `TraceIdFilter` and `TraceContext`

When changing API behavior, check whether the behavior is enforced in these shared modules before editing an individual service.

### Service shape

Each business service is a conventional Spring Boot application with its own `application.yml`, port, and persistence configuration. The current default local ports are:

- gateway: `8080`
- user-service: `8081`
- product-service: `8082`
- cart-service: `8083`
- order-service: `8084`
- stock-service: `8085`
- coupon-service: `8086`

Within a service, code generally follows this layering:

- `controller`: external HTTP endpoints
- `controller.admin`: admin endpoints
- `controller.internal`: service-to-service endpoints
- `service` / `service.impl`: application logic
- `mapper`: MyBatis-Plus database access
- `entity`: persistence models (`*DO`)
- `dto` / `vo`: request and response models

`user-service` and `product-service` are the most developed modules and are the best references for existing patterns.

### API and security model

Controllers return the shared `Result` envelope rather than raw DTOs. Exceptions are normalized centrally by `GlobalExceptionHandler`, and responses can carry a trace ID populated by `TraceIdFilter`.

Authentication is annotation-driven:

- `SecurityAutoConfiguration` registers `JwtAuthenticationInterceptor` for MVC requests
- the interceptor only authenticates endpoints/classes marked with `@RequireLogin`
- JWT parsing and login context live in `common-security`

If an endpoint should require authentication, the expected pattern is to add `@RequireLogin` at method or controller level instead of wiring security inside the controller.

### Gateway and service discovery

`gateway-service` is a Spring Cloud Gateway WebFlux app. Its routes map `/api/v1/**` paths to backend services using `lb://<service-name>` URIs.

Important local-dev detail: the checked-in `application.yml` files disable Nacos discovery/config by default (`spring.cloud.nacos.discovery.enabled: false` and `config.enabled: false`). That means:

- individual services can start locally without Nacos
- the gateway's `lb://...` routes depend on service discovery, so gateway-based end-to-end routing requires enabling discovery and registering services
- if discovery is still disabled, test service endpoints by calling each service directly on its local port instead of going through the gateway

### Persistence and integration patterns

The services use:

- MyBatis-Plus for database access
- MySQL as the primary relational store
- Redis / Redisson for cache and token-related state
- Nacos for service discovery/config when enabled
- OpenFeign for service-to-service HTTP calls

A concrete cross-service pattern already exists in `cart-service`: `ProductClient` is a Feign client for `product-service`, and it calls the internal endpoint exposed by `ProductInternalController`. Follow that split when adding inter-service APIs: keep internal contracts separate from public storefront/admin endpoints.

## Configuration conventions

Every application uses this profile pattern:

- `spring.profiles.active` defaults to `local`
- `application.yml` imports `application-${spring.profiles.active}-private.yml`

So environment-specific connection details are expected to live in the private profile file, while the checked-in `application.yml` contains the stable application structure.

## Testing notes

Current tests are module-local JUnit 5 + Mockito unit tests under `src/test/java`. No `@SpringBootTest` integration-test pattern is present in the repository today, so existing test conventions are fast unit tests around service logic rather than full application boot tests.