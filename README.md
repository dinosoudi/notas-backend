# notas-backend

para levantar todo lo de terraform:
cd terraform
terraform apply -var-file="terraform.tfvars"

Flujo:
terraform apply   ← infra arriba
git push a prod   ← GitHub Actions deploya
pruebas           ← verificas que todo funciona 
terraform destroy ← bajas todo


Host: postgres (nombre del servicio, no localhost)
Port: 5432
Database: taskflow
User: taskflow_user
Password: taskflow_pass


docker:
docker-compose up -d
docker-compose ps


3 horas más en norte de virginia us-east-1 que en mi casa


# Taskflow Backend

API REST para gestión de notas y tareas, construida con Java 21 + Spring Boot 3.5. Desplegada en AWS con ECS Fargate, RDS PostgreSQL y entrega continua vía GitHub Actions.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | JWT (stateless, sin sesiones) |
| Cloud | AWS — ECS Fargate, RDS, ECR, ALB, Secrets Manager |
| Infra como código | Terraform |
| CI/CD | GitHub Actions |
| Contenedores (local) | Docker Compose |

## Estructura del proyecto

```
src/main/java/com/taskflow/
├── auth/          # Registro, login, verificación de email, refresh tokens
├── notes/         # CRUD de notas con paginación
├── tags/          # CRUD de etiquetas con borrado en cascada
├── users/         # Perfil, cambio de contraseña, eliminación de cuenta
└── shared/
    ├── config/    # Spring Security, JWT, AWS
    ├── security/  # JwtFilter, JwtService
    ├── email/     # SES (AWS Simple Email Service)
    └── exception/ # Manejo global de errores
```

## Requisitos locales

- Docker Desktop
- Java 21 + Maven (solo si corres fuera de Docker)

## Levantar en local con Docker

```bash
# 1. Copiar el archivo de variables de entorno
cp .env.example .env

# 2. Levantar postgres + pgadmin + backend
docker-compose up --build

# 3. Verificar que todo esté corriendo
docker-compose ps
```

La API queda disponible en `http://localhost:8080/api/v1`

Para bajar los contenedores:
```bash
docker-compose down        # conserva los datos de la DB
docker-compose down -v     # borra también los datos (empezar desde cero)
```

## Variables de entorno

Copia `.env.example` a `.env` y ajusta los valores. Las variables con default no son obligatorias para correr en local.

| Variable | Descripción | Default |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `taskflow` |
| `DB_USER` | Usuario de PostgreSQL | `taskflow_user` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `taskflow_pass` |
| `JWT_SECRET` | Clave secreta para firmar JWT (mín. 64 chars) | default de desarrollo |
| `SERVER_PORT` | Puerto del servidor | `8080` |
| `AWS_SES_FROM_EMAIL` | Email remitente para notificaciones | — |
| `AWS_S3_BUCKET` | Bucket S3 para archivos | — |
| `APP_URL` | URL base de la app (para links en emails) | — |

> **Nota:** `AWS_SES_FROM_EMAIL`, `AWS_S3_BUCKET` y `APP_URL` solo son necesarias si vas a usar envío de emails y subida de archivos.

## pgAdmin

Disponible en `http://localhost:5050` una vez levantado Docker Compose.

Para conectar al servidor de PostgreSQL desde pgAdmin:

| Campo | Valor |
|---|---|
| Host | `postgres` (nombre del servicio Docker, no `localhost`) |
| Port | `5432` |
| Database | `taskflow` |
| Username | `taskflow_user` |
| Password | `taskflow_pass` |

## Perfiles de Spring

| Perfil | Cuándo se usa |
|---|---|
| `dev` | Local con IntelliJ o Docker Compose |
| `prod` | ECS Fargate en AWS |
| `test` | Tests con Testcontainers |

## Migraciones (Flyway)

Las migraciones están en `src/main/resources/db/migration/` y se ejecutan automáticamente al arrancar la app.

| Versión | Descripción |
|---|---|
| V1 | Tabla de usuarios |
| V2 | Tabla de refresh tokens |
| V3 | Tabla de etiquetas |
| V4 | Tabla de notas |
| V5 | Stored procedure: borrado en cascada de tags |
| V6 | Stored procedure: resumen de notas |
| V7 | Columnas para reset de contraseña |

## Endpoints principales

Todos los endpoints tienen el prefijo `/api/v1`.

```
POST   /auth/register
POST   /auth/login
POST   /auth/verify-email
POST   /auth/forgot-password
POST   /auth/reset-password
POST   /auth/refresh
POST   /auth/logout

GET    /users/me
PATCH  /users/me/name
PATCH  /users/me/phone
PATCH  /users/me/preferences
PATCH  /users/me/password
DELETE /users/me

GET    /notes
POST   /notes
GET    /notes/{id}
PUT    /notes/{id}
DELETE /notes/{id}

GET    /tags
POST   /tags
PUT    /tags/{id}
DELETE /tags/{id}
```

## Infraestructura AWS

La infra se gestiona con Terraform desde la carpeta `/terraform`.

```
VPC
├── Subnets públicas  → ALB
└── Subnets privadas  → ECS Fargate + RDS PostgreSQL

ECR              → Repositorio de imágenes Docker
ECS Fargate      → Contenedor de la app (512 CPU / 1024 MB)
RDS PostgreSQL   → Base de datos (db.t3.micro)
ALB              → Load balancer público
Secrets Manager  → JWT secret + contraseña de RDS
CloudWatch       → Logs del contenedor (/ecs/taskflow, retención 7 días)
```

### Levantar la infra

```bash
cd terraform
terraform apply -var-file="terraform.tfvars"
```

### Bajar la infra

```bash
terraform destroy -var-file="terraform.tfvars"
```

> **Importante:** `terraform destroy` borra todo — RDS, ECS, ECR, VPC. Los datos de la base de datos se pierden.

## CI/CD

| Rama | Pipeline | Acción |
|---|---|---|
| `main` / PRs | `ci.yml` | Build, tests, análisis SonarCloud |
| `prod` | `deploy.yml` | Build JAR → imagen Docker → push a ECR → deploy a ECS |

El deploy a ECS sigue este flujo:
1. Build del JAR con Maven
2. Build y push de la imagen Docker a ECR (tag = git SHA + latest)
3. Descarga la Task Definition actual de ECS
4. Inyecta la nueva imagen en la Task Definition
5. Registra la nueva Task Definition y hace el deploy
6. Espera a que ECS confirme que el servicio está estable

## Diferencia horaria

La región `us-east-1` (Norte de Virginia) tiene **3 horas más** que Ciudad de México (UTC-6 vs UTC-5 en horario de verano). Los timestamps en CloudWatch aparecen en UTC.