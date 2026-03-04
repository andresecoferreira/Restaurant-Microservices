# 🍽️ Restaurant Microservices

A restaurant management system built on a **microservices** architecture, developed as the final project for the **Distributed Computing** course — Universidade Lusófona.


---

## 📋 Table of Contents

- [Architecture](#-architecture)
- [Services](#-services)
- [Technologies](#-technologies)
- [Inter-Service Communication](#-inter-service-communication)
- [Prerequisites](#-prerequisites)
- [How to Run](#-how-to-run)
- [API Endpoints](#-api-endpoints)
- [Project Structure](#-project-structure)

---

## 🏗️ Architecture

The system follows a microservices architecture with 4 independent services, each with its own PostgreSQL database. Asynchronous communication between services is handled via **Apache Kafka** and synchronous communication via **OpenFeign (REST)**.

```
┌─────────────────┐     REST (Feign)     ┌─────────────────────┐
│   Reservation    │ ──────────────────►  │    Restaurant        │
│   Service :8082  │                      │    Service :8081     │
└────────┬─────────┘                      └──────────────────────┘
         │
         │  Kafka Events
         │  (reservation.created / confirmed / cancelled)
         │
    ┌────▼────────────┐          ┌──────────────────────┐
    │  Notification    │          │   Analytics           │
    │  Service :8083   │          │   Service :8084       │
    └─────────────────┘          └───────────────────────┘
```

---

## 📦 Services

### 1. Restaurant Service (`:8081`)
Manages restaurants, menus, and availability time slots.

- Restaurant CRUD operations
- Menu item CRUD operations
- Availability slot management (create, book, release)
- Database migrations with **Flyway**

### 2. Reservation Service (`:8082`)
Manages reservations with synchronous communication to the Restaurant Service and event publishing via Kafka.

- Create, confirm, cancel, and delete reservations
- Communicates with the Restaurant Service via **OpenFeign** to verify/book slots
- Publishes Kafka events: `reservation.created`, `reservation.confirmed`, `reservation.cancelled`
- Message envelope with `traceId` for traceability

### 3. Notification Service (`:8083`)
Kafka consumer that processes reservation events and generates notifications.

- Listens to topics: `reservation.created`, `reservation.confirmed`, `reservation.cancelled`
- Persists notifications in the database
- Publishes `restaurant.notified` event after processing

### 4. Analytics Service (`:8084`)
Business Intelligence service that consumes Kafka events to generate statistics and dashboards.

- Most popular restaurants (by confirmed reservations)
- VIP customers (by total bookings)
- Occupancy by date (total guests, reservations, average party size)
- Reservation status distribution (PENDING, CONFIRMED, CANCELLED)
- Static web dashboard (`index.html`)

---

## 🛠️ Technologies

| Technology | Version | Usage |
|---|---|---|
| **Java** | 24 | Main language |
| **Spring Boot** | 3.4.4 | Backend framework |
| **Spring Cloud OpenFeign** | 2024.0.0 | REST communication between services |
| **Spring gRPC** | 0.12.0 | gRPC communication |
| **Apache Kafka** | 7.5.0 (Confluent) | Asynchronous messaging |
| **PostgreSQL** | 17 | Relational database |
| **Flyway** | 11.12 | Database migrations |
| **Docker / Docker Compose** | - | Containerization |
| **Swagger / OpenAPI** | - | API documentation |
| **Maven** | 3.9.9 | Dependency management |
| **Lombok** | - | Boilerplate reduction |

---

## 🔗 Inter-Service Communication

### Synchronous (REST via OpenFeign)
- **Reservation → Restaurant**: Availability check, slot booking, and slot release.

### Asynchronous (Apache Kafka)

| Topic | Producer | Consumers |
|---|---|---|
| `reservation.created` | Reservation Service | Notification Service, Analytics Service |
| `reservation.confirmed` | Reservation Service | Notification Service, Analytics Service |
| `reservation.cancelled` | Reservation Service | Notification Service, Analytics Service |
| `restaurant.notified` | Notification Service | (Audit log) |

All messages use a **MessageEnvelope** with:
- `eventType` — event type
- `status` — reservation status
- `traceId` — traceability ID (UUID)
- `occurredAt` — event timestamp
- `payload` — event data

---

## ⚙️ Prerequisites

- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- [Java 24](https://jdk.java.net/24/) (for local development)
- [Maven 3.9+](https://maven.apache.org/) (for local development)

---

## 🚀 How to Run

### 1. Create the shared Docker network

```bash
docker network create shared-backend-network
```

### 2. Start the services (in order)

Each service has its own `compose.yml`. A `.env` file is required in each service folder with the necessary environment variables.

**Example `.env`:**
```env
PORT=8081
APPLICATION_NAME=restaurant-service
POSTGRES_DB=restaurant_db
POSTGRES_USER=project_user
POSTGRES_PASSWORD=project_secure_password_2024
```

**Start the Reservation Service first** (includes Kafka and Zookeeper):

```bash
cd reservation
docker compose up -d
```

**Then start the remaining services:**

```bash
cd ../restaurant
docker compose up -d

cd ../notification
docker compose up -d

cd ../analytics
docker compose up -d
```

### 3. Verify the services are running

- Restaurant Service: http://localhost:8081/actuator/health
- Reservation Service: http://localhost:8082/actuator/health
- Notification Service: http://localhost:8083/actuator/health
- Analytics Service: http://localhost:8084/actuator/health
- Kafka UI: http://localhost:8080

---

## 📡 API Endpoints

### Restaurant Service (`:8081`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/restaurants` | Create restaurant |
| `GET` | `/api/restaurants` | List all restaurants |
| `GET` | `/api/restaurants/{id}` | Get restaurant by ID |
| `PUT` | `/api/restaurants/{id}` | Update restaurant |
| `DELETE` | `/api/restaurants/{id}` | Delete restaurant |
| `POST` | `/api/restaurants/{id}/slots` | Create availability slot |
| `GET` | `/api/restaurants/{id}/slots` | List slots for a restaurant |
| `GET` | `/api/slots/{id}` | Get slot by ID |
| `POST` | `/api/menu/items/{restaurantId}` | Create menu item |
| `GET` | `/api/menu/items/{id}` | Get menu item |
| `GET` | `/api/menu/restaurants/{restaurantId}` | List menu for a restaurant |
| `PUT` | `/api/menu/items/{id}` | Update menu item |
| `DELETE` | `/api/menu/items/{id}` | Delete menu item |

### Reservation Service (`:8082`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/reservations` | Create reservation |
| `GET` | `/api/reservations` | List all reservations |
| `GET` | `/api/reservations/{id}` | Get reservation by ID |
| `POST` | `/api/reservations/{id}/confirm` | Confirm reservation |
| `POST` | `/api/reservations/{id}/cancel` | Cancel reservation |
| `DELETE` | `/api/reservations/{id}` | Delete reservation |

### Analytics Service (`:8084`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/popular-restaurants` | Most popular restaurants |
| `GET` | `/api/analytics/vip-customers` | VIP customers |
| `GET` | `/api/analytics/occupancy-by-date` | Occupancy by date |
| `GET` | `/api/analytics/status-distribution` | Status distribution |

### Swagger Documentation

Each service provides interactive documentation:
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- API Docs (JSON): `http://localhost:{port}/api-docs`

---

## 📁 Project Structure

```
Restaurant Microservices/
├── restaurant/                    # Restaurant Service (:8081)
│   ├── compose.yml
│   ├── Dockerfile
│   ├── pom.xml
│   ├── migrations/
│   │   └── V1__initial_schema.sql
│   └── src/main/java/.../restaurant_service/
│       ├── controller/            # REST Controllers
│       ├── dto/                   # Data Transfer Objects
│       ├── model/                 # JPA Entities
│       ├── repository/            # Spring Data Repositories
│       └── service/               # Business Logic
│
├── reservation/                   # Reservation Service (:8082)
│   ├── compose.yml                # Includes Kafka, Zookeeper & Kafka UI
│   ├── Dockerfile
│   ├── pom.xml
│   ├── migrations/
│   │   └── V1__initial_schema.sql
│   └── src/main/java/.../
│       ├── client/                # Feign Clients (REST → Restaurant)
│       ├── controller/            # REST Controllers
│       ├── dto/                   # Data Transfer Objects
│       ├── event/                 # Event DTOs (MessageEnvelope)
│       ├── kafka/                 # Kafka Producers
│       ├── model/                 # JPA Entities
│       ├── repository/            # Spring Data Repositories
│       └── service/               # Business Logic
│
├── notification/                  # Notification Service (:8083)
│   ├── compose.yml
│   ├── Dockerfile
│   ├── pom.xml
│   ├── migrations/
│   │   └── V1__create_notification_table.sql
│   └── src/main/java/.../notification_service/
│       ├── controller/            # REST Controllers
│       ├── dto/                   # DTOs & Event Payloads
│       ├── kafka/                 # Kafka Consumers & Producers
│       ├── model/                 # JPA Entities
│       └── repository/            # Spring Data Repositories
│
├── analytics/                     # Analytics Service (:8084)
│   ├── compose.yml
│   ├── Dockerfile
│   ├── pom.xml
│   ├── migrations/
│   │   └── V1__create_analytics_schema.sql
│   └── src/main/java/.../analytics_service/
│       ├── controller/            # REST Controllers (Analytics API)
│       ├── dto/                   # DTOs (Popular, VIP, Occupancy, Status)
│       ├── kafka/                 # Kafka Consumers
│       ├── model/                 # JPA Entities
│       ├── repository/            # Spring Data Repositories
│       └── service/               # Analytics Business Logic
│
└── README.md
```

---

## 📊 Monitoring

- **Spring Actuator** — Health, info, and metrics endpoints available at `/actuator/health`
- **Kafka UI** — Web interface for monitoring Kafka topics and messages at http://localhost:8080
