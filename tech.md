# Technical Design

## Goal

Build a small event-ingestion system consisting of two services, a Kafka broker, and PostgreSQL 16. Drone accepts protobuf messages over HTTP and publishes them to Kafka. DBService consumes those messages and persists them in PostgreSQL.

## Components

- **Drone**: Spring Boot service exposing `POST /sendProtoBuf/`. It accepts a serialized protobuf request, validates it, and publishes the message to Kafka. On successful publication it returns a success response; invalid input or Kafka failures return an appropriate non-success HTTP status.
- **Kafka**: Broker for transferring drone messages from Drone to DBService. Both services connect using the internal Compose network. Use a configured topic for drone messages.
- **DBService**: Spring Boot service consuming the configured Kafka topic, decoding each protobuf message, and writing its fields to PostgreSQL. It acknowledges a Kafka record only after the database write succeeds, so failed writes can be retried.
- **PostgreSQL 16**: Database used by DBService, with its data stored in a Compose-managed volume.

## Protobuf message

Define a protobuf message for a drone record with these fields:

| Field | Type |
|---|---|
| `id` | `int64` |
| `pos1` | `string` |
| `pos2` | `string` |

Use the same generated protobuf definition in both services. The HTTP endpoint accepts the serialized protobuf body with the `application/x-protobuf` content type.

## Persistence

DBService stores `id`, `pos1`, and `pos2` in a PostgreSQL table. Use `id` as the primary key. Database connection details are supplied through environment variables rather than hard-coded credentials.

## Docker Compose

`compose.yaml` runs Kafka, PostgreSQL 16, Drone, and DBService:

- **Kafka** runs as a single-node KRaft broker. Services on the Compose network connect to `kafka:9092`; tools running on the host connect to `localhost:29092`.
- **PostgreSQL 16** is available on `localhost:5432`. The defaults are database/user/password `drone`; override them with `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.
- **Drone** is built from `drone-service/Dockerfile`, listens on `localhost:8080`, and waits for Kafka health. `POST /sendProtoBuf/` accepts an `application/x-protobuf` `DronePosition` body and publishes it to the `drone-positions` topic.
- **DBService** is built from `db-service/Dockerfile`, listens on port `8081`, waits for healthy Kafka and PostgreSQL services, and consumes the `drone-positions` topic. It creates the `drone_position` table on startup and upserts records by `id`. Kafka offsets are committed only after the database write succeeds; failures are retried. WebSocket clients can connect to `ws://localhost:8081/ws`; after a record is saved, connected clients receive `message saved to db <id>`.
- Kafka and PostgreSQL have health checks and persistent named volumes. The default database credentials are for local development only and must be changed for any shared or deployed environment.

### Local startup steps

1. Build and start the services with `docker compose up --build -d`.
2. Check readiness with `docker compose ps`; wait until Kafka and PostgreSQL report healthy and both application services are running.
3. Send serialized `DronePosition` protobuf messages to `http://localhost:8080/sendProtoBuf/` with the `application/x-protobuf` content type.
4. Stop the services with `docker compose down`. Data remains in the named volumes; `docker compose down -v` removes that data.
