# Multimodal App Entry Backend

This folder contains the current backend baseline for the multimodal app entry project:

- Spring Boot 3.5.x
- Java 17
- Maven Wrapper
- PostgreSQL
- MyBatis-Plus
- Flyway
- Actuator

## What is already in place

- Application entrypoint and health endpoints
- `application.yml` with PostgreSQL and Flyway defaults
- Flyway migrations `V1` to `V5`
- Configuration-center CRUD for:
  - `mm_function_config`
  - `mm_model_service`
  - `mm_function_model_binding`
  - `mm_param_template`

## Local prerequisites

- JDK 17
- PostgreSQL 14+

`mvn` does not need to be on `PATH` anymore. Use the committed Maven Wrapper instead.

## Configure the database

The default connection settings live in `src/main/resources/application.yml`.

Recommended environment variables:

```powershell
$env:DB_URL="jdbc:postgresql://127.0.0.1:5432/multimodal_app_dev?currentSchema=app"
$env:DB_USERNAME="mm_app"
$env:DB_PASSWORD="your_password"
```

## Flyway note

If you point this project at a schema that was already created manually, `baseline-on-migrate: true` allows Flyway to attach without forcing a rebuild.

If you want a fully clean verification run, create a new empty database and start the application against that database once.

## Run

From this folder:

```powershell
.\mvnw.cmd spring-boot:run
```

If port `8080` is already occupied, either stop the old process or override the port:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## Verify

After startup, check:

- `http://127.0.0.1:8080/api/ping`
- `http://127.0.0.1:8080/actuator/health`

From the workspace root, you can also run the smoke script:

```powershell
.\scripts\smoke-config-center.ps1
```

## Current milestone

The backend is ready for configuration-center integration closure. The next milestone after this phase is the workbench shell plus the minimum session/message/task backbone.
