# 🚀 MSCMS — Complete Setup Guide (Docker Only)

This file contains **everything** you need to run the entire Multi-Sport Club Management System on a fresh machine with only Docker installed. No source code, no IDE, no Java needed.

---

## 📋 Prerequisites

- **Docker Desktop** installed and running.
- Internet connection (to pull images).

---

## 🛠️ Step 1: Create a Project Folder

```bash
mkdir mscms
cd mscms
```

---

## 📄 Step 2: Create the Required Files

You need to create **3 files** inside the `mscms` folder. Copy-paste each one exactly.

---

### File 1: `docker-compose.yml`

Create a file called `docker-compose.yml` and paste this:

```yaml
services:
  # --- Infrastructure ---
  postgres:
    image: postgres:14
    container_name: postgres
    environment:
      POSTGRES_USER: embarkx
      POSTGRES_PASSWORD: embarkx
    volumes:
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U embarkx"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - mscms-network

  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    networks:
      - mscms-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    ports:
      - "9092:9092"
    networks:
      - mscms-network

  keycloak:
    image: quay.io/keycloak/keycloak:26.2.5
    container_name: keycloak
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin123
    command: ["start-dev", "--import-realm"]
    volumes:
      - ./keycloak/mscms-realm.json:/opt/keycloak/data/import/mscms-realm.json
    ports:
      - "8443:8080"
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/localhost/8080 && echo done"]
      interval: 10s
      timeout: 5s
      retries: 12
    networks:
      - mscms-network

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - mscms-network

  # --- Spring Cloud Infrastructure ---
  eureka-server:
    image: ghcr.io/tefaa1/mscms/eureka-server:latest
    container_name: eureka-server
    ports:
      - "8761:8761"
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8761/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - mscms-network

  config-server:
    image: ghcr.io/tefaa1/mscms/config-server:latest
    container_name: config-server
    ports:
      - "8082:8082"
    depends_on:
      eureka-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - mscms-network

  gateway-service:
    image: ghcr.io/tefaa1/mscms/gateway-service:latest
    container_name: gateway-service
    restart: on-failure
    ports:
      - "8080:8080"
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - SPRING_DATA_REDIS_HOST=redis
    depends_on:
      config-server:
        condition: service_healthy
      keycloak:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - mscms-network

  # --- Microservices ---
  user-management-service:
    image: ghcr.io/tefaa1/mscms/user-management-service:latest
    container_name: user-management-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_user
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - KEYCLOAK_USER=admin
      - KEYCLOAK_PASSWORD=admin123
      - KEYCLOAK_REALM=mscms
      - APP_ADMIN_KEYCLOAK_ID=default-admin-id
      - APP_ADMIN_FIRST_NAME=Admin
      - APP_ADMIN_LAST_NAME=User
      - APP_ADMIN_USERNAME=admin
      - APP_ADMIN_EMAIL=admin@mscms.com
      - APP_ADMIN_PASSWORD=admin123
      - APP_ADMIN_PHONE=0000000000
      - APP_ADMIN_AGE=30
      - APP_ADMIN_GENDER=MALE
      - APP_ADMIN_ADDRESS=System
      - APP_ADMIN_BLOOD_TYPE=O_POSITIVE
      - APP_ADMIN_ROLE=ADMIN
      - APP_ADMIN_CREATE_DEFAULT=true
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    networks:
      - mscms-network

  player-management-service:
    image: ghcr.io/tefaa1/mscms/player-management-service:latest
    container_name: player-management-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_player
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
    networks:
      - mscms-network

  training-match-service:
    image: ghcr.io/tefaa1/mscms/training-match-service:latest
    container_name: training-match-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_training
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
    networks:
      - mscms-network

  medical-fitness-service:
    image: ghcr.io/tefaa1/mscms/medical-fitness-service:latest
    container_name: medical-fitness-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_medical
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
    networks:
      - mscms-network

  notification-mail-service:
    image: ghcr.io/tefaa1/mscms/notification-mail-service:latest
    container_name: notification-mail-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_notification
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - mscms-network

  reports-analytics-service:
    image: ghcr.io/tefaa1/mscms/reports-analytics-service:latest
    container_name: reports-analytics-service
    restart: on-failure
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8082
      - DB_URI=jdbc:postgresql://postgres:5432/mscms_reports
      - DB_USER=embarkx
      - DB_PASSWORD=embarkx
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      config-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
    networks:
      - mscms-network

networks:
  mscms-network:
    driver: bridge
```

---

### File 2: `init-db.sql`

Create a file called `init-db.sql` and paste this:

```sql
CREATE DATABASE mscms_user;
CREATE DATABASE mscms_player;
CREATE DATABASE mscms_training;
CREATE DATABASE mscms_medical;
CREATE DATABASE mscms_notification;
CREATE DATABASE mscms_reports;

GRANT ALL PRIVILEGES ON DATABASE mscms_user TO embarkx;
GRANT ALL PRIVILEGES ON DATABASE mscms_player TO embarkx;
GRANT ALL PRIVILEGES ON DATABASE mscms_training TO embarkx;
GRANT ALL PRIVILEGES ON DATABASE mscms_medical TO embarkx;
GRANT ALL PRIVILEGES ON DATABASE mscms_notification TO embarkx;
GRANT ALL PRIVILEGES ON DATABASE mscms_reports TO embarkx;
```

---

### File 3: `keycloak/mscms-realm.json`

First create the folder, then the file:

```bash
mkdir keycloak
```

Create `keycloak/mscms-realm.json` and paste this:

```json
{
  "realm": "mscms",
  "enabled": true,
  "sslRequired": "none",
  "registrationAllowed": true,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": false,
  "accessTokenLifespan": 604800,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000,
  "roles": {
    "realm": [
      { "name": "ADMIN", "description": "Administrator role" },
      { "name": "COACH", "description": "Coach role" },
      { "name": "PLAYER", "description": "Player role" },
      { "name": "SCOUT", "description": "Scout role" },
      { "name": "SPONSOR", "description": "Sponsor role" },
      { "name": "FAN", "description": "Fan role" },
      { "name": "STAFF", "description": "Staff role" },
      { "name": "SPORT_MANAGER", "description": "Sport Manager role" },
      { "name": "TEAM_MANAGER", "description": "Team Manager role" },
      { "name": "HEAD_COACH", "description": "Head Coach role" },
      { "name": "ASSISTANT_COACH", "description": "Assistant Coach role" },
      { "name": "SPECIFIC_COACH", "description": "Specific Coach role" },
      { "name": "DOCTOR", "description": "Doctor role" },
      { "name": "PHYSIOTHERAPIST", "description": "Physiotherapist role" },
      { "name": "FITNESS_COACH", "description": "Fitness Coach role" },
      { "name": "PERFORMANCE_ANALYST", "description": "Performance Analyst role" },
      { "name": "TEAM_DOCTOR", "description": "Team Doctor role" },
      { "name": "NATIONAL_TEAM", "description": "National Team role" }
    ]
  },
  "clients": [
    {
      "clientId": "mscms-frontend",
      "name": "MSCMS Frontend App",
      "enabled": true,
      "publicClient": true,
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": true,
      "redirectUris": [
        "http://localhost:3000/*",
        "http://localhost:5173/*",
        "http://localhost:4200/*",
        "http://localhost:8080/*"
      ],
      "webOrigins": [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:4200",
        "http://localhost:8080",
        "*"
      ],
      "protocol": "openid-connect"
    }
  ],
  "users": [
    {
      "username": "admin",
      "email": "admin@mscms.com",
      "firstName": "Admin",
      "lastName": "User",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "admin123",
          "temporary": false
        }
      ],
      "realmRoles": ["ADMIN"],
      "clientRoles": {
        "realm-management": [
          "realm-admin",
          "manage-users",
          "query-users",
          "query-groups",
          "manage-clients",
          "view-users"
        ]
      }
    }
  ]
}
```

---

## 📁 Final Folder Structure

Your `mscms` folder should look like this:

```
mscms/
├── docker-compose.yml
├── init-db.sql
└── keycloak/
    └── mscms-realm.json
```

---

## 📥 Step 3: Pull the Docker Images

```bash
docker compose pull
```

This downloads all pre-built images from GitHub Container Registry (GHCR). No login required.

---

## 🚀 Step 4: Start Everything

```bash
docker compose up -d
```

Wait **2–3 minutes** for all services to start and register with each other.

---

## ✅ Step 5: Verify It's Running

| What | URL | Notes |
| :--- | :--- | :--- |
| **Eureka Dashboard** | http://localhost:8761 | All 8 services should show as `UP` |
| **Swagger UI (API Docs)** | http://localhost:8080/swagger-ui.html | Interactive API testing |
| **Keycloak Admin** | http://localhost:8443 | User: `admin` / Pass: `admin123` |

---

## 🔐 How to Authenticate

### Default Admin Account
- **Username:** `admin`
- **Password:** `admin123`

### Login (get JWT token)
```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```
The response will contain an `access_token`. Use it in all subsequent requests.

> **Note for frontend developers:** Always use the gateway's `/auth/login` endpoint to obtain tokens — do **not** call Keycloak directly at `localhost:8443`. The gateway gets tokens from the internal Keycloak and the JWT issuer must match the internal URL. Tokens obtained from `/auth/login` work seamlessly with all API endpoints.

### Signup (create a new FAN user)
```
POST http://localhost:8080/auth/signup
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "MyPassword123",
  "firstName": "John",
  "lastName": "Doe",
  "displayName": "JohnDoe",
  "phone": "+1234567890",
  "age": 25,
  "gender": "MALE",
  "address": "123 Main St",
  "bloodType": "O_POSITIVE"
}
```
Signup always creates a **FAN** role. To create users with other roles, use the admin endpoint:

### Admin: Create user with specific role
```
POST http://localhost:8080/auth/admin/create-user
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "username": "coachsmith",
  "email": "coach@example.com",
  "password": "CoachPass123",
  "firstName": "Coach",
  "lastName": "Smith",
  "displayName": "CoachSmith",
  "phone": "+1987654321",
  "age": 40,
  "gender": "MALE",
  "address": "456 Stadium Rd",
  "bloodType": "A_POSITIVE",
  "role": "HEAD_COACH"
}
```
Available roles: `ADMIN`, `COACH`, `PLAYER`, `SCOUT`, `SPONSOR`, `FAN`, `STAFF`, `SPORT_MANAGER`, `TEAM_MANAGER`, `HEAD_COACH`, `ASSISTANT_COACH`, `SPECIFIC_COACH`, `DOCTOR`, `PHYSIOTHERAPIST`, `FITNESS_COACH`, `PERFORMANCE_ANALYST`, `TEAM_DOCTOR`, `NATIONAL_TEAM`

### Using the Token
Add this header to every request:
```
Authorization: Bearer <YOUR_TOKEN_HERE>
```

---

## 🛡️ Role Permissions

The API Gateway enforces role-based security. If a role doesn't have access, you get `403 Forbidden`.

| Endpoints | Allowed Roles |
| :--- | :--- |
| **Medical & Fitness** | |
| `/injuries/**` | ADMIN, TEAM_DOCTOR, PHYSIOTHERAPIST, HEAD_COACH |
| `/diagnoses/**` | ADMIN, TEAM_DOCTOR |
| `/treatments/**`, `/rehabilitations/**`, `/recovery-programs/**` | ADMIN, TEAM_DOCTOR, PHYSIOTHERAPIST |
| `/fitness-tests/**` | ADMIN, TEAM_DOCTOR, FITNESS_COACH |
| `/training-loads/**` | ADMIN, FITNESS_COACH, HEAD_COACH, PERFORMANCE_ANALYST |
| **Training & Match** | |
| `/training-sessions/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH, SPECIFIC_COACH, FITNESS_COACH |
| `/training-plans/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH |
| `/training-drills/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH, SPECIFIC_COACH |
| `/training-attendance/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH |
| `/player-training-assessments/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH, SPECIFIC_COACH, FITNESS_COACH |
| `/matches/**` | ADMIN, HEAD_COACH, ASSISTANT_COACH, PERFORMANCE_ANALYST |
| `/match-events/**` | ADMIN, HEAD_COACH, PERFORMANCE_ANALYST |
| `/match-formations/**`, `/match-lineups/**` | ADMIN, HEAD_COACH |
| `/match-performance-reviews/**`, `/player-match-statistics/**` | ADMIN, HEAD_COACH, PERFORMANCE_ANALYST |
| **Player Management** | |
| `/teams/**` | ADMIN, SPORT_MANAGER, TEAM_MANAGER, HEAD_COACH |
| `/sports/**` | ADMIN, SPORT_MANAGER |
| `/rosters/**` | ADMIN, HEAD_COACH, TEAM_MANAGER |
| `/player-contracts/**` | ADMIN, SPORT_MANAGER, TEAM_MANAGER |
| `/player-transfers-incoming/**`, `/player-transfers-outgoing/**` | ADMIN, SPORT_MANAGER |
| `/player-callups/**` | ADMIN, HEAD_COACH, NATIONAL_TEAM |
| `/players/**` (GET) | ADMIN, HEAD_COACH, ASSISTANT_COACH, SPECIFIC_COACH, FITNESS_COACH, PERFORMANCE_ANALYST, TEAM_DOCTOR, PHYSIOTHERAPIST, TEAM_MANAGER |
| `/players/**` (POST/DELETE) | ADMIN only |
| `/players/**` (PUT) | ADMIN, HEAD_COACH |
| `/outer-players/**`, `/outer-teams/**` | ADMIN, SCOUT |
| **User Management** | |
| `/sport-managers/**` | ADMIN, SPORT_MANAGER |
| `/team-managers/**` | ADMIN, SPORT_MANAGER, TEAM_MANAGER |
| `/staff/**` | ADMIN, SPORT_MANAGER, TEAM_MANAGER |
| `/scouts/**` | ADMIN, SCOUT |
| `/sponsors/**` | ADMIN, SPONSOR |
| `/national-teams/**` | ADMIN, NATIONAL_TEAM |
| `/fans/**` | ADMIN, FAN |
| **Notification & Mail** | |
| `/notifications/**`, `/messages/**` | Any authenticated user |
| `/alerts/**` | ADMIN, HEAD_COACH, TEAM_DOCTOR |
| **Reports & Analytics** | |
| `/match-analyses/**`, `/team-analytics/**`, `/training-analytics/**` | ADMIN, HEAD_COACH, PERFORMANCE_ANALYST |
| `/player-analytics/**` | ADMIN, HEAD_COACH, PERFORMANCE_ANALYST, SCOUT |
| `/scout-reports/**` | ADMIN, SCOUT |
| `/sponsor-offers/**` | ADMIN, SPONSOR |

---

## 🔔 Notable API Details

- **`PATCH /alerts/{id}/acknowledge`** requires query parameter `acknowledgedByKeycloakId`:
  ```
  PATCH http://localhost:8080/alerts/1/acknowledge?acknowledgedByKeycloakId=<KEYCLOAK_ID>
  ```
- **`GET /players`** requires query parameter `status` (values: `AVAILABLE`, `INJURED`, `ABSENT`, `SUSPENDED`):
  ```
  GET http://localhost:8080/players?status=AVAILABLE
  ```
- **`DELETE /match-formations/{id}`** safely detaches any linked matches before deleting.

---

## 📖 Using Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Select a service from the dropdown (e.g., "Medical & Fitness").
3. **Important:** Select **"API Gateway"** from the **Servers** dropdown.
4. Click **Authorize** (top right), paste your JWT token, click **Authorize**.
5. Now you can "Try it out" on any endpoint.

---

## 🛑 Useful Commands

```bash
# Stop everything
docker compose down

# Stop and remove all data (fresh start)
docker compose down -v

# View logs of a specific service
docker compose logs -f gateway-service

# Restart a single service
docker compose restart user-management-service

# Pull latest images and restart
docker compose pull
docker compose up -d
```

---

## ⚠️ Troubleshooting

### Only some services appear in Eureka?
- Wait 2-3 more minutes — slower machines take longer.
- Check logs: `docker compose logs -f user-management-service`
- If a service keeps restarting, run `docker compose down -v` for a fresh start.

### Login returns 401?
- Make sure Eureka shows all services as `UP` first.
- Use the correct credentials: `admin` / `admin123`.

### Database errors?
- Run `docker compose down -v` to wipe all data and start clean.
- Then `docker compose up -d` again.

---

## 🏗️ Architecture Overview

```
Browser/Frontend (React, Angular, etc.)
        │
        ▼
  API Gateway (:8080)  ◄── JWT validation + role-based security
        │
        ├── User Management Service
        ├── Player Management Service
        ├── Training & Match Service
        ├── Medical & Fitness Service
        ├── Notification & Mail Service
        └── Reports & Analytics Service
        
  Supporting Infrastructure:
  ├── Config Server (:8082) — centralized config
  ├── Eureka Server (:8761) — service discovery
  ├── Keycloak (:8443) — identity provider
  ├── PostgreSQL (:5432) — database
  ├── Redis (:6379) — caching & rate limiting
  ├── RabbitMQ (:15672) — messaging
  └── Kafka (:9092) — event streaming
```
