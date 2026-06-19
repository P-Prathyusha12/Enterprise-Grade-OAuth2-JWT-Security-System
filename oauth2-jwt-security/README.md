# Enterprise OAuth2 JWT Security System

A **production-grade, single Spring Boot application** implementing JWT-based authentication and authorization with MySQL, Redis, RBAC, multi-tenancy, and audit logging.

---

## 🚀 Quick Start

### Step 1 — MySQL Setup
Ensure MySQL is running on port 3306. The app **creates the database automatically** on first startup.

Update credentials in `src/main/resources/application.yml`:
```yaml
spring.datasource.username: root
spring.datasource.password: password   # ← change this
```

### Step 2 — Redis Setup
Ensure Redis is running on port 6379 (default). If not installed, download from https://redis.io/download

### Step 3 — Import into Eclipse
1. Open Eclipse → **File → Import → Existing Maven Projects**
2. Browse to `C:\Users\pprat\eclipse-workspace\oauth2-jwt-security`
3. Click **Finish**
4. Right-click project → **Maven → Update Project**
5. Run `SecurityApplication.java` as a Java Application

### Step 4 — Test
Open: http://localhost:8080/swagger-ui.html

---

## 🔑 Default Users (Auto-Seeded on First Run)

| Username | Password    | Role        |
|----------|-------------|-------------|
| `admin`  | `Admin@1234`| ROLE_ADMIN  |
| `user`   | `User@1234` | ROLE_USER   |

---

## 📡 API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/auth/login` | No | Login → get access + refresh token |
| POST | `/auth/register` | No | Register new user |
| POST | `/auth/refresh` | No | Get new access token via refresh token |
| POST | `/auth/logout` | JWT | Revoke tokens + blacklist in Redis |
| GET | `/users` | JWT + ADMIN | Get all users |
| GET | `/users/me` | JWT | Get my profile |
| GET | `/users/{id}` | JWT + ADMIN | Get user by ID |
| DELETE | `/users/{id}` | JWT + ADMIN | Delete user |
| GET | `/admin/dashboard` | JWT + ADMIN | System stats |
| GET | `/admin/audit-logs` | JWT + ADMIN | All audit logs |
| GET | `/admin/audit-logs/{user}` | JWT + ADMIN | Logs by user |
| GET | `/swagger-ui.html` | No | Swagger UI |

---

## 🧪 Testing with Postman
1. Import `postman_collection.json` into Postman
2. Run **#1 Login (admin)** — `access_token` is auto-saved
3. All subsequent requests use `{{access_token}}` automatically

---

## 🛡️ Features
- ✅ JWT with custom claims (`tenant_id`, `roles_permissions`)
- ✅ Refresh Token (stored in DB with expiry)
- ✅ Token Blacklisting via Redis on logout
- ✅ RBAC with `@PreAuthorize`
- ✅ Multi-Tenancy (`tenant_id` claim in JWT)
- ✅ BCrypt password hashing
- ✅ Audit logging (login, logout, failures)
- ✅ Global exception handler (clean JSON errors)
- ✅ Swagger UI at `/swagger-ui.html`
