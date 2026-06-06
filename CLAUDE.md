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
- Start local infrastructure (MySQL 8.0, Redis 7, Nacos v2.4.2 standalone):
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

Spring Boot **3.5.13** / Spring Cloud **2025.0.2** / Spring Cloud Alibaba **2023.0.3.4** on Java 17. Maven dependency management is centralized in the root `pom.xml`.

### Module layout

The root `pom.xml` aggregates three groups:

- `sparkle-common` — shared libraries: `common-core` (pure primitives, no Spring) and `common-starter-web` (bundles web, security, logging, Feign integration)
- `sparkle-gateway` — `gateway-service`, a Spring Cloud Gateway WebFlux app
- `sparkle-services` — six business microservices: `user-service`, `product-service`, `cart-service`, `order-service`, `stock-service`, `coupon-service`

### Shared foundation (`sparkle-common`)

**`common-core`** — framework-free primitives:
- `Result` — universal API response envelope (`code`, `msg`, `data`, `traceId`); static error code constants (40000, 40100, 40300, 40400, 40900, 50000)
- `PageResponse<T>` — paginated response container (`list`, `total`, `pageNo`, `pageSize`)
- `BusinessException(int code, String message)` — runtime exception caught by `GlobalExceptionHandler`
- `TraceContext` — ThreadLocal trace-id holder

**`common-starter-web`** — bundles web, security, logging, and Feign in one module:

- *Web layer* — `Results` factory for building `ResponseEntity<Result>` (auto-injects trace-id, derives HTTP status from error code prefix); `GlobalExceptionHandler` (`@RestControllerAdvice`) maps Spring/validation/persistence exceptions to `Result` error responses
- *Security layer* — `JwtTokenService` (generate, authenticate, blacklist, invalidate-user-tokens); `TokenUser` value object; `LoginUserContext` ThreadLocal holder; `RequestUserContextInterceptor` (reads `X-User-Id`/`X-User-Type`/`X-Token-Id` headers and populates context — never blocks); `SecurityFeignRequestInterceptor` (propagates user+trace headers on outgoing Feign calls)
- *Logging layer* — `TraceIdFilter` (OncePerRequestFilter that seeds TraceContext from header or generates a new one; writes back to response)

Auto-configuration (`SecurityAutoConfiguration`, `SecuritySharedAutoConfiguration`) is discovered via `@ComponentScan("com.sparkleshop")` — there is no `spring.factories` or `AutoConfiguration.imports` file.

### Authentication flow (gateway → service)

1. **Gateway**: `GatewayAuthenticationFilter` (global filter, `HIGHEST_PRECEDENCE`) parses the `Authorization: Bearer <jwt>` header via `JwtTokenService.authenticate()`; checks a configurable whitelist (unauthenticated paths); checks role-based path guarding (admin vs member paths); on success strips the raw JWT and propagates `X-User-Id`, `X-User-Type`, `X-Token-Id`, `X-Trace-Id` headers downstream. On failure writes a JSON `Result` error directly to the response.

2. **Services**: `RequestUserContextInterceptor` reads the gateway-set headers and populates `LoginUserContext`. Service code calls `LoginUserContext.getRequired()` or `LoginUserContext.getRequiredUserId()` programmatically — there is no `@RequireLogin` annotation.

3. **Feign calls**: `SecurityFeignRequestInterceptor` relays `X-User-Id`, `X-User-Type`, `X-Token-Id`, `X-Trace-Id` headers to downstream services, preserving the user context across the call chain.

### Service internals

Each business service follows this layering:

| Layer | Package | Role |
|-------|---------|------|
| Public controller | `controller` | External HTTP endpoints; returns `ResponseEntity<Result>` via `Results` |
| Admin controller | `controller.admin` | Back-office endpoints under `/admin/**` |
| Internal controller | `controller.internal` | Service-to-service endpoints under `/internal/**`, called via Feign |
| Feign clients | `api/<service-name>/` | Interfaces annotated `@FeignClient` calling internal endpoints |
| Business logic | `service` / `service.impl` | Application logic |
| Database access | `mapper` | MyBatis-Plus `BaseMapper` interfaces |
| Entities | `entity` | `*DO` classes with `@TableName`, `@TableId`, `@TableLogic` |
| DTOs/VOs | `dto`, `vo` | Request/response models (see naming conventions below) |

Default local ports: gateway `8080`, user `8081`, product `8082`, cart `8083`, order `8084`, stock `8085`, coupon `8086`.

`user-service` and `product-service` are the most developed modules — reference them for patterns. `order-service` is the central orchestrator with 5 Feign clients calling other services.

### DTO / VO naming conventions

| Purpose | Suffix | Package | Example |
|---------|--------|---------|---------|
| API response to client | `*RespVO` | `vo/` | `ProductPageRespVO` |
| Internal Feign request | `*Request` | `dto/internal/` | `CouponValidateRequest` |
| Internal Feign response | `*RespDTO` | `dto/internal/` | `ProductSkuSnapshotRespDTO` |
| Admin request body | `*Request` | `dto/admin/` | `AdminSpuCreateRequest` |
| Admin response | `*Response` | `dto/admin/` | `AdminUserDetailResponse` |
| Page/filter query params | `*DTO` / `*QueryDTO` | `dto/` | `ProductPageQueryDTO` |

There is **no common base entity class** — each DO repeats `id`, `createTime`, `updateTime`, `deleted` fields manually.

### Feign client conventions

- All Feign clients live in `api/<called-service-name>/` under the calling service
- All methods use `@PostMapping` with `@RequestBody` — no GET-based Feign calls
- All return `Result<T>` (the shared envelope)
- Internal controllers use `@RequestMapping({"", "/<service-name>"})` so paths work with or without a context prefix

### MyBatis-Plus conventions

- Every mapper extends `BaseMapper<XxxDO>`
- **No XML mapper files** — all queries are `default` methods using `LambdaQueryWrapper` / `LambdaUpdateWrapper`
- `@TableLogic` on `deleted` field enables soft-delete on all entities
- Services that need pagination define a `MybatisPlusConfig` registering `PaginationInnerInterceptor(DbType.MYSQL)`
- CAS-style (compare-and-swap) updates use `LambdaUpdateWrapper.eq(DO::getStatus, expectedStatus)` for optimistic concurrency

### Distributed lock pattern (Redisson)

Services that need mutual exclusion use Redisson `tryLock` with a **short timeout** — never `lock.lock()`, which can cause infinite waiting (see `错误修复日志.md` 2026-04-18). The canonical pattern:

```java
RLock lock = redissonClient.getLock(RedisKeys.someLockKey(args));
try {
    if (!lock.tryLock(3, TimeUnit.SECONDS)) {
        throw new BusinessException(Result.CONFLICT, "操作频繁，请稍后再试");
    }
    // ... critical section ...
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new BusinessException(Result.SERVER_ERROR, "操作被中断");
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

Key points:
- Lock keys are defined in `*RedisKeys` constants classes (e.g., `UserRedisKeys`, `CouponRedisKeys`)
- Timeout is 3 seconds by default
- Always check `lock.isHeldByCurrentThread()` before unlocking
- Always handle `InterruptedException` and restore the interrupt flag

### Redis key and error code conventions

- Each service defines a `<Service>RedisKeys` constants class (e.g., `CartRedisKeys`, `CouponRedisKeys`, `UserRedisKeys`) with static methods generating key strings. Use `hutool UUID` for unique coupon codes and other generated identifiers.
- Each service defines a `<Service>ErrorCodes` constants class (e.g., `CouponErrorCodes`, `OrderErrorCodes`) with static int constants consumed by `BusinessException`.
- `cart-service` stores cart data in **Redis only** (no MySQL cart tables) — cart entities have both `*DO` (for API models) and `*CacheDO` (for Redis hash serialization) forms.

### Coupon service: scheduled task

`CouponServiceImpl` uses `@Scheduled` to periodically mark expired coupons. The schedule is configured via `@EnableScheduling` in the application class.

## Configuration conventions

- `spring.profiles.active` defaults to `local`
- `application.yml` imports `application-${spring.profiles.active}-private.yml` for secrets
- Nacos discovery and config are **disabled by default** (`spring.cloud.nacos.discovery.enabled: false`, `config.enabled: false`) — services start locally without Nacos; enable them when you need gateway-based routing or centralized config
- When discovery is disabled, call service endpoints directly on their local ports instead of going through the gateway

## Testing notes

Current tests are JUnit 5 + Mockito unit tests under `src/test/java`. No `@SpringBootTest` integration tests exist in the repository. Conventions are fast unit tests around service logic.

## Design documentation

The project root (`sparkmall`) contains a `开发文档/` directory with comprehensive design docs (in Chinese):

- `星火优选-后端开发文档.md` — backend architecture and implementation details
- `星火优选-管理端开发文档.md` — admin panel design
- `星火优选-管理端前后端接口联调文档.md` — admin API integration
- `星火优选-前端开发文档.md` — frontend design
- `星火优选数据库设计.md` — database schema design
- `星火优选详细功能清单.md` — feature list
- `已完成模块后续优化清单.md` — post-completion optimization items

The root also contains `错误修复日志.md` which records confirmed bug fixes — check it before working on areas with past issues (e.g., distributed lock infinite wait in `user-service`).