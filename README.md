<div align="center">

# Spring Auth Keycloak

<p>
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Keycloak-26.0.10-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/OAuth2-JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
    ![CI](https://github.com/BrahimNajiCode/spring-auth-keycloak/actions/workflows/ci.yml/badge.svg)
</p>

**A production-ready Spring Boot authentication service that delegates identity management to Keycloak, exposing clean OAuth2/JWT-secured REST endpoints for registration, login, token refresh, and user management.**

</div>

---

## Table of Contents

- [Features](#features)
- [Architecture & Workflow](#architecture--workflow)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Environment Variables](#environment-variables)
- [Project Structure](#project-structure)
- [Security Best Practices](#security-best-practices)

---

## Features

- **Keycloak-Delegated Auth**: Registration and credential management are fully handled by Keycloak via its Admin REST API — no passwords stored locally.
- **JWT Resource Server**: Every protected endpoint validates Bearer tokens signed by Keycloak using RS256.
- **Custom Role Extraction**: A bespoke `JwtAuthenticationConverter` reads both `realm_access.roles` and `resource_access.<client>.roles` from the JWT and maps them to Spring's `GrantedAuthority` — so `@PreAuthorize("hasRole('ADMIN')")` works out of the box.
- **ROPC Login Flow**: Users log in with username/password; the service proxies the request to Keycloak's token endpoint and returns an `access_token` / `refresh_token` pair.
- **Token Refresh & Revocation**: Seamless token refresh and best-effort refresh-token revocation (logout) via Keycloak's `/logout` endpoint.
- **Local User Mirror**: A PostgreSQL `users` table mirrors Keycloak identities (linked by `keycloak_id`) for local relational joins and audit purposes.
- **Global Exception Handling**: Centralized `GlobalExceptionHandler` maps domain exceptions (`NotFoundException`, `ConflictException`, `UnauthorizedException`, `KeycloakException`) to structured `ApiErrorResponse` payloads.
- **Validated Request DTOs**: All incoming payloads are validated with Bean Validation (`@NotBlank`, `@Email`, `@Pattern`) before reaching the service layer.
- **Typed Configuration**: `KeycloakProperties` is a `@ConfigurationProperties` record validated at startup — no scattered `@Value` annotations.
- **Actuator Observability**: `/actuator/health`, `/actuator/info`, and `/actuator/metrics` exposed for health checks and monitoring.
- **CORS Ready**: Centralized `CorsConfig` bean for explicit origin control in production.
- **Stateless & CSRF-Safe**: Session creation is `STATELESS`; CSRF is disabled (JWT-based auth is inherently immune).

---

## Architecture & Workflow

1. **Infrastructure Boot** — Docker Compose starts PostgreSQL 17 and Keycloak 26. Keycloak waits for a healthy Postgres instance before starting (health-check + `depends_on` condition).
2. **Keycloak Realm Setup** — A realm (`Auth`) is pre-configured with a confidential client (`auth-spring-client`) used by the Spring backend for Admin API calls, and a public client used by end-users.
3. **User Registration** — The client calls `POST /api/v1/auth/register`. Spring Boot validates the DTO, then calls Keycloak's Admin REST API (authenticated via `client_credentials` grant using `KeycloakAdminClient`) to create the user in the realm. A local `User` record is persisted in PostgreSQL, linked by `keycloak_id`.
4. **User Login** — The client calls `POST /api/v1/auth/login`. The service proxies the credentials to Keycloak's token endpoint using the **ROPC grant** (`grant_type=password`). Keycloak returns a signed JWT access token and a refresh token.
5. **Authenticated Request** — The client sends the `Authorization: Bearer <access_token>` header. Spring Security's OAuth2 Resource Server intercepts the request, fetches Keycloak's JWKS to verify the RS256 signature, then passes the decoded `Jwt` through the custom `JwtAuthenticationConverter` to extract realm + client roles into `SecurityContextHolder`.
6. **Authorization** — Method-level security (`@EnableMethodSecurity`) allows fine-grained access control via `@PreAuthorize`. Admin-only paths are additionally guarded at the filter-chain level with `.hasRole("ADMIN")`.
7. **Token Refresh** — `POST /api/v1/auth/refresh` exchanges a refresh token for a new token pair via `grant_type=refresh_token`.
8. **Logout** — Calls Keycloak's `/logout` endpoint to revoke the refresh token server-side. This is best-effort; errors are logged but not re-thrown.

---

## Tech Stack

- **Core**
  - Java 21
  - Spring Boot 4.1.0 (Web MVC, Data JPA, Validation, Actuator)
  - Lombok

- **Security**
  - Spring Security (OAuth2 Resource Server, OAuth2 Client)
  - Keycloak 26.0.10 (`keycloak-admin-client`)
  - JWT / RS256 — JWKS auto-fetched from Keycloak

- **Database**
  - PostgreSQL 17
  - Hibernate / Spring Data JPA

- **Infrastructure & Tools**
  - Docker & Docker Compose
  - Maven (with Maven Wrapper)
  - Spring Boot Actuator

---

## Getting Started

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java (JDK) | 21 |
| Docker Desktop | 24+ |
| Docker Compose | v2.x |
| Maven | 3.9+ (or use the included `mvnw`) |

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/BrahimNajiCode/spring-auth-keycloak.git
   cd spring-auth-keycloak
   ```

2. **Configure environment variables:**

   Copy the example env file and fill in your values:
   ```bash
   cp .env.example .env
   ```
   Edit `.env` with your chosen credentials (see [Environment Variables](#environment-variables) below).

3. **Start the infrastructure (Postgres + Keycloak):**
   ```bash
   docker compose up -d
   ```
   Keycloak will be available at `http://localhost:8081`. Postgres will be healthy before Keycloak starts.

4. **Configure the Keycloak Realm:**

   > [!WARNING]
   > If your client is confidential (Client authentication: ON), you must include the client_secret in all token requests — including login and refresh.

   - Open the Keycloak Admin Console at `http://localhost:8081` and log in with your `KEYCLOAK_ADMIN` credentials.
   - Create a new realm named **`Auth`**.
   - Create a **confidential client** (`auth-spring-client`) with `client_credentials` grant enabled.
     - **Settings tab:**
       - Client authentication: ON
       - Authorization: OFF
       - Authentication flow: Standard flow ✓, Direct access grants ✓, Service accounts roles ✓
     - **Credentials tab:**
       - Client Authenticator: Client Id and Secret
       - Copy the secret and set it as KEYCLOAK_ADMIN_CLIENT_SECRET
     - **Service accounts roles tab:**
       - Filter by clients → realm-management
       - Assign: manage-users, view-users
     - **Client scopes tab:**
       - Ensure these are present: roles, profile, email, openid
   - Create a **public client** for your front-end / testing (e.g. `my-app`) with `password` grant enabled.
   - Create the required roles (e.g. `ROLE_USER`, `ROLE_ADMIN`) in the realm.

5. **Run the Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   Or on Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```
   The API will be available at `http://localhost:8080`.

6. **Verify the service is healthy:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## API Endpoints

### Auth

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user (creates user in Keycloak + local DB) | Public |
| `POST` | `/api/v1/auth/login` | Log in with username & password, returns JWT token pair | Public |
| `POST` | `/api/v1/auth/refresh` | Exchange a refresh token for a new access token | Public |
| `POST` | `/api/v1/auth/logout` | Revoke the refresh token (server-side logout) | Bearer token |
| `POST` | `/api/v1/auth/forgot-password` | Trigger a Keycloak password-reset email | Public |

### Users

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Get the currently authenticated user's profile | Bearer token |
| `PUT` | `/api/v1/users/me` | Update the currently authenticated user's profile | Bearer token |

### Admin

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | List all users | `ROLE_ADMIN` |
| `DELETE` | `/api/v1/admin/users/{id}` | Delete a user | `ROLE_ADMIN` |

### Observability

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application health status |
| `GET` | `/actuator/info` | Application info |
| `GET` | `/actuator/metrics` | Runtime metrics |

---

## Environment Variables

Create a `.env` file at the project root. **Never commit this file.** (It is already in `.gitignore`.)

```dotenv
# -- Docker Compose -----------------------------------------------------------
POSTGRES_USER=your_db_user
POSTGRES_PASSWORD=your_db_password
POSTGRES_DB=auth_keycloak_db

KEYCLOAK_ADMIN=your_admin_username
KEYCLOAK_ADMIN_PASSWORD=your_admin_password

# -- Spring Boot (application.yaml) ------------------------------------------
KEYCLOAK_SERVER_URL=http://localhost:8081
KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/Auth
KEYCLOAK_TOKEN_URI=http://localhost:8081/realms/Auth/protocol/openid-connect/token

REALM=Auth
CLIENT_ID=auth-spring-client
CLIENT_SECRET=your_client_secret_from_keycloak
AUTH_GRANT_TYPE=client_credentials
SCOPE=openid

# Public client ID (the client your front-end / users authenticate against)
KEYCLOAK_PUBLIC_CLIENT_ID=my-app
```

> The application loads this file automatically via `spring.config.import: optional:file:.env[.properties]`.

---

## Project Structure

```
spring-auth-keycloak/
├── docker/
│   └── postgresql/
│       └── init.sql                  # DB initialization script
├── src/
│   └── main/
│       ├── java/com/brahim/auth/
│       │   ├── SpringAuthKeycloakApplication.java   # Entry point
│       │   ├── config/
│       │   │   ├── CorsConfig.java                  # CORS configuration
│       │   │   ├── KeycloakAdminClientConfig.java   # Keycloak Admin REST client bean
│       │   │   ├── KeycloakProperties.java          # @ConfigurationProperties record
│       │   │   └── SecurityConfig.java              # Security filter chain & route rules
│       │   ├── controller/
│       │   │   ├── AuthController.java              # /api/v1/auth/** endpoints
│       │   │   └── UserController.java              # /api/v1/users/** endpoints
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── LoginRequest.java
│       │   │   │   ├── RefreshTokenRequest.java
│       │   │   │   └── RegisterRequest.java         # Bean-validated record
│       │   │   └── response/
│       │   │       ├── ApiErrorResponse.java        # Standardized error body
│       │   │       ├── TokenResponse.java           # access_token / refresh_token
│       │   │       └── UserResponse.java
│       │   ├── exception/
│       │   │   ├── ConflictException.java           # 409 - duplicate user
│       │   │   ├── KeycloakException.java           # Keycloak Admin API errors
│       │   │   ├── NotFoundException.java           # 404
│       │   │   └── UnauthorizedException.java       # 401
│       │   ├── handler/
│       │   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice
│       │   ├── model/
│       │   │   └── User.java                        # JPA entity (mirrors Keycloak user)
│       │   ├── repository/
│       │   │   └── UserRepository.java              # Spring Data JPA repository
│       │   ├── security/
│       │   │   ├── JwtAccessDeniedHandler.java      # 403 handler
│       │   │   ├── JwtAuthenticationConverter.java  # Keycloak -> Spring roles bridge
│       │   │   ├── JwtAuthenticationEntryPoint.java # 401 handler
│       │   │   └── SecurityContextHelper.java       # Current-user helper
│       │   └── service/
│       │       ├── AuthService.java                 # Auth service interface
│       │       ├── KeycloakTokenService.java        # Token service interface
│       │       ├── UserService.java                 # User service interface
│       │       └── impl/
│       │           ├── AuthServiceImpl.java
│       │           ├── KeycloakTokenServiceImpl.java # ROPC / refresh / revoke
│       │           └── UserServiceImpl.java
│       └── resources/
│           └── application.yaml                     # All app configuration
├── docker-compose.yml                               # Postgres + Keycloak services
├── pom.xml                                          # Maven dependencies
└── .env                                             # Local secrets - never commit
```

---

## Security Best Practices

> [!CAUTION]
> **Never commit your `.env` file or any file containing real secrets.** This includes `CLIENT_SECRET`, database passwords, and Keycloak admin credentials.

- `.env` is listed in `.gitignore` — always verify before pushing with `git status`.
- Use `.env.example` (with placeholder values) to document required variables for collaborators.
- In production, prefer a secrets manager (e.g., HashiCorp Vault, AWS Secrets Manager, or Kubernetes Secrets) over `.env` files.
- Rotate `CLIENT_SECRET` regularly from the Keycloak Admin Console.
- Restrict the Keycloak `client_credentials` client to the minimum required scopes.
- Always run Keycloak behind TLS in any environment beyond local development.

---

<div align="center">
  <p>Developed by <a href="https://github.com/BrahimNajiCode">Brahim Naji</a></p>
</div>
