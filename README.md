# Kubee Platform

Backend monorepo for Kubee: shared libraries plus independently deployable Spring Boot services.
The Angular apps live in the separate `kubee-workspace` repo.

```
kubee-platform/
├── pom.xml                 parent: Spring Boot 4, Java 21, shared versions
├── libs/
│   ├── kubee-common/       ResponseResource, CommonResponse, Status, CommonException, CommonSerializable
│   └── kubee-security/     JWT validation, JwtAuthentication, UserContext, UserContextUtil (auto-configured)
└── services/
    ├── auth/               users, tenants, apps, privileges, subscriptions          port 8080, schema auth
    ├── inventory/          Kubee Inventory                                         port 8085, schema inventory
    └── pos/                Kubee POS                                               port 8086, schema pos
```

Each service is its own jar, Docker image and deployment, with its own Flyway migrations in its own
Postgres schema. All three can share one Postgres database.

History: `services/auth`, `services/inventory` and `services/pos` were imported with their full git history
from their original standalone repos.

All Java code lives under `com.kubee`: `com.kubee.auth`, `com.kubee.inventory`, `com.kubee.pos`,
`com.kubee.common`, `com.kubee.security`.

## Build

```bash
./mvnw package -DskipTests                          # everything
./mvnw -pl services/pos -am package -DskipTests     # one service (+ the libs it needs)
./mvnw -pl services/pos -am test                    # one service's tests
```

## Run locally

Each service reads its config from environment variables (`application.properties`). For local work, put
your values in `services/<name>/src/main/resources/application-local.properties` (git-ignored) and run with
the `local` profile, e.g. in IntelliJ: `-Dspring.profiles.active=local`.

All services must use the **same `jwt.secret`**: auth signs the tokens, the others only validate them.

## Docker

Images are built from the repository root so they can include the shared libraries:

```bash
docker build -f services/auth/Dockerfile      -t kubee-auth .
docker build -f services/inventory/Dockerfile -t kubee-inventory .
docker build -f services/pos/Dockerfile       -t kubee-pos .
```

On Render (or similar), set the service's root directory to the repo root and its Dockerfile path to
`services/<name>/Dockerfile`.

## Shared libraries

**kubee-common**: the response envelope and exceptions every API uses.
`import com.kubee.common.ResponseResource;`, `com.kubee.common.CommonException`, ...

**kubee-security**: adding the dependency is enough; `KubeeSecurityAutoConfiguration` registers
`JwtTokenProvider`, a request-scoped `UserContext` and `JwtAuthFilter`. Each service keeps its own
`SecurityConfig` (public paths, CORS) and adds the filter:

```java
http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

In code, use `UserContextUtil`: `getTenantIdOrThrow()`, `getUserUuid()`, `isPlatformUser()`,
`requireTenantAccess(tenantId)`, ...

A service can add its own authorities to the signed-in user by declaring a `JwtAuthorityContributor`
bean (POS uses this for `POS_MANAGE`, see `PosPermissions`).

## Adding a service

1. Create `services/<name>` with a `pom.xml` whose parent is `com.kubee:kubee-platform` (relativePath `../../pom.xml`)
   and depend on `kubee-common` / `kubee-security`.
2. Add `<module>services/<name></module>` to the root `pom.xml`.
3. Copy a service `Dockerfile` and change the module path.
