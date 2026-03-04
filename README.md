# 🍽️ Restaurant Microservices

Sistema de gestão de restaurantes baseado em arquitetura de **microserviços**, desenvolvido como projeto final da cadeira de **Computação Distribuída** — Universidade Lusófona.

**Alunos:** a22304646, a22308720

---

## 📋 Índice

- [Arquitetura](#-arquitetura)
- [Serviços](#-serviços)
- [Tecnologias](#-tecnologias)
- [Comunicação entre Serviços](#-comunicação-entre-serviços)
- [Pré-requisitos](#-pré-requisitos)
- [Como Executar](#-como-executar)
- [API Endpoints](#-api-endpoints)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🏗️ Arquitetura

O sistema segue uma arquitetura de microserviços com 4 serviços independentes, cada um com a sua própria base de dados PostgreSQL. A comunicação assíncrona entre serviços é feita via **Apache Kafka** e a comunicação síncrona via **OpenFeign (REST)**.

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

## 📦 Serviços

### 1. Restaurant Service (`:8081`)
Gestão de restaurantes, menus e slots de disponibilidade (time slots).

- CRUD de restaurantes
- CRUD de itens de menu
- Gestão de slots de disponibilidade (criar, reservar, libertar)
- Migrações de base de dados com **Flyway**

### 2. Reservation Service (`:8082`)
Gestão de reservas com comunicação síncrona ao Restaurant Service e publicação de eventos via Kafka.

- Criar, confirmar, cancelar e eliminar reservas
- Comunicação com o Restaurant Service via **OpenFeign** para verificar/reservar slots
- Publicação de eventos Kafka: `reservation.created`, `reservation.confirmed`, `reservation.cancelled`
- Envelope de mensagens com `traceId` para rastreabilidade

### 3. Notification Service (`:8083`)
Consumidor Kafka que processa eventos de reserva e gera notificações.

- Escuta tópicos: `reservation.created`, `reservation.confirmed`, `reservation.cancelled`
- Persiste notificações na base de dados
- Publica evento `restaurant.notified` após processamento

### 4. Analytics Service (`:8084`)
Serviço de Business Intelligence que consome eventos Kafka para gerar estatísticas e dashboards.

- Restaurantes mais populares (por reservas confirmadas)
- Clientes VIP (por total de reservas)
- Ocupação por data (total de hóspedes, reservas, tamanho médio)
- Distribuição de estados de reserva (PENDING, CONFIRMED, CANCELLED)
- Dashboard web estático (`index.html`)

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Utilização |
|---|---|---|
| **Java** | 24 | Linguagem principal |
| **Spring Boot** | 3.4.4 | Framework backend |
| **Spring Cloud OpenFeign** | 2024.0.0 | Comunicação REST entre serviços |
| **Spring gRPC** | 0.12.0 | Comunicação gRPC |
| **Apache Kafka** | 7.5.0 (Confluent) | Mensageria assíncrona |
| **PostgreSQL** | 17 | Base de dados relacional |
| **Flyway** | 11.12 | Migrações de base de dados |
| **Docker / Docker Compose** | - | Containerização |
| **Swagger / OpenAPI** | - | Documentação da API |
| **Maven** | 3.9.9 | Gestão de dependências |
| **Lombok** | - | Redução de boilerplate |

---

## 🔗 Comunicação entre Serviços

### Síncrona (REST via OpenFeign)
- **Reservation → Restaurant**: Verificação de disponibilidade, reserva e libertação de slots.

### Assíncrona (Apache Kafka)

| Tópico | Produtor | Consumidores |
|---|---|---|
| `reservation.created` | Reservation Service | Notification Service, Analytics Service |
| `reservation.confirmed` | Reservation Service | Notification Service, Analytics Service |
| `reservation.cancelled` | Reservation Service | Notification Service, Analytics Service |
| `restaurant.notified` | Notification Service | (Audit log) |

Todas as mensagens utilizam um **MessageEnvelope** com:
- `eventType` — tipo do evento
- `status` — estado da reserva
- `traceId` — ID de rastreabilidade (UUID)
- `occurredAt` — timestamp do evento
- `payload` — dados do evento

---

## ⚙️ Pré-requisitos

- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- [Java 24](https://jdk.java.net/24/) (para desenvolvimento local)
- [Maven 3.9+](https://maven.apache.org/) (para desenvolvimento local)

---

## 🚀 Como Executar

### 1. Criar a rede Docker partilhada

```bash
docker network create shared-backend-network
```

### 2. Iniciar os serviços (por ordem)

Cada serviço tem o seu próprio `compose.yml`. Requer um ficheiro `.env` em cada pasta de serviço com as variáveis de ambiente necessárias.

**Exemplo de `.env`:**
```env
PORT=8081
APPLICATION_NAME=restaurant-service
POSTGRES_DB=restaurant_db
POSTGRES_USER=project_user
POSTGRES_PASSWORD=project_secure_password_2024
```

**Iniciar o Reservation Service primeiro** (inclui Kafka e Zookeeper):

```bash
cd reservation
docker compose up -d
```

**Depois iniciar os restantes serviços:**

```bash
cd ../restaurant
docker compose up -d

cd ../notification
docker compose up -d

cd ../analytics
docker compose up -d
```

### 3. Verificar que os serviços estão a correr

- Restaurant Service: http://localhost:8081/actuator/health
- Reservation Service: http://localhost:8082/actuator/health
- Notification Service: http://localhost:8083/actuator/health
- Analytics Service: http://localhost:8084/actuator/health
- Kafka UI: http://localhost:8080

---

## 📡 API Endpoints

### Restaurant Service (`:8081`)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/restaurants` | Criar restaurante |
| `GET` | `/api/restaurants` | Listar todos os restaurantes |
| `GET` | `/api/restaurants/{id}` | Obter restaurante por ID |
| `PUT` | `/api/restaurants/{id}` | Atualizar restaurante |
| `DELETE` | `/api/restaurants/{id}` | Eliminar restaurante |
| `POST` | `/api/restaurants/{id}/slots` | Criar slot de disponibilidade |
| `GET` | `/api/restaurants/{id}/slots` | Listar slots de um restaurante |
| `GET` | `/api/slots/{id}` | Obter slot por ID |
| `POST` | `/api/menu/items/{restaurantId}` | Criar item de menu |
| `GET` | `/api/menu/items/{id}` | Obter item de menu |
| `GET` | `/api/menu/restaurants/{restaurantId}` | Listar menu de um restaurante |
| `PUT` | `/api/menu/items/{id}` | Atualizar item de menu |
| `DELETE` | `/api/menu/items/{id}` | Eliminar item de menu |

### Reservation Service (`:8082`)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/reservations` | Criar reserva |
| `GET` | `/api/reservations` | Listar todas as reservas |
| `GET` | `/api/reservations/{id}` | Obter reserva por ID |
| `POST` | `/api/reservations/{id}/confirm` | Confirmar reserva |
| `POST` | `/api/reservations/{id}/cancel` | Cancelar reserva |
| `DELETE` | `/api/reservations/{id}` | Eliminar reserva |

### Analytics Service (`:8084`)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/analytics/popular-restaurants` | Restaurantes mais populares |
| `GET` | `/api/analytics/vip-customers` | Clientes VIP |
| `GET` | `/api/analytics/occupancy-by-date` | Ocupação por data |
| `GET` | `/api/analytics/status-distribution` | Distribuição de estados |

### Documentação Swagger

Cada serviço disponibiliza documentação interativa:
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- API Docs (JSON): `http://localhost:{port}/api-docs`

---

## 📁 Estrutura do Projeto

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
│   ├── compose.yml                # Inclui Kafka, Zookeeper e Kafka UI
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

## 📊 Monitorização

- **Spring Actuator** — Endpoints de health, info e metrics disponíveis em `/actuator/health`
- **Kafka UI** — Interface web para monitorizar tópicos e mensagens Kafka em http://localhost:8080

