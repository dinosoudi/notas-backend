# notas-backend

para levantar todo lo de terraform:
cd terraform
terraform apply -var-file="terraform.tfvars"

Flujo:
terraform apply   ← infra arriba
git push a prod   ← GitHub Actions deploya
pruebas           ← verificas que todo funciona 
terraform destroy ← bajas todo

cd terraform

# Ver qué va a crear sin crear nada
terraform plan -var-file="terraform.tfvars"

# Crear toda la infra (~10 minutos, RDS es lo más lento)
terraform apply -var-file="terraform.tfvars"



Host: postgres (nombre del servicio, no localhost)
Port: 5432
Database: taskflow
User: taskflow_user
Password: taskflow_pass


docker:
docker-compose up -d
docker-compose ps
para coerrer test para pruebas de estres/carga/profiling usamos el comando:
k6 run k6/load-test.js


3 horas más en norte de virginia us-east-1 que en mi casa

Estructura de carpetas con tree y con tree /F para archivos

| Servicio | URL | Credenciales               |
|---|---|----------------------------| 
| API REST | `http://localhost:8080/api/v1` | —                          |
| pgAdmin | `http://localhost:5050` | admin@taskflow.com / admin |
| Prometheus | `http://localhost:9090` | —                          |
| Grafana | `http://localhost:3001` | admin / Hesoyam:3          |


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
| Observabilidad | Prometheus + Grafana |
| Logs | Logback + Logstash encoder (JSON en prod, texto en dev) |
| Pruebas de carga | k6 |

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

# 2. Levantar todos los servicios
#    postgres + pgadmin + backend + prometheus + grafana
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
| `AWS_REGION` | Región de AWS | `us-east-1` |
| `AWS_ACCESS_KEY_ID` | Access key de AWS (solo dev con Docker, para SES) | — |
| `AWS_SECRET_ACCESS_KEY` | Secret key de AWS (solo dev con Docker, para SES) | — |
| `AWS_SES_FROM_EMAIL` | Email remitente para notificaciones | — |
| `APP_URL` | URL base del frontend (para links en emails) | — |

> **Nota sobre credenciales AWS:**
> - **IntelliJ/local:** el SDK lee automáticamente `~/.aws/credentials` configurado con `aws configure` — no necesitas variables de entorno
> - **Docker Compose:** el contenedor no tiene acceso a `~/.aws/credentials` del host, por eso necesita `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` en el `.env`
> - **ECS Fargate (prod):** el rol IAM de la task provee las credenciales automáticamente — no se necesita ninguna key

## Servicios disponibles con Docker Compose

Una vez levantado `docker-compose up`, tienes acceso a:

| Servicio | URL | Credenciales |
|---|---|---|
| API REST | `http://localhost:8080/api/v1` | — |
| pgAdmin | `http://localhost:5050` | admin@taskflow.com / admin |
| Prometheus | `http://localhost:9090` | — |
| Grafana | `http://localhost:3001` | admin / admin (te pide cambiarla al primer login) |

### Conectar pgAdmin a PostgreSQL

Una vez dentro de pgAdmin, agrega un nuevo servidor con estos datos:

| Campo | Valor |
|---|---|
| Host | `postgres` (nombre del servicio Docker, no `localhost`) |
| Port | `5432` |
| Database | `taskflow` |
| Username | `taskflow_user` |
| Password | `taskflow_pass` |

## Observabilidad (Prometheus + Grafana)

### Cómo funciona

```
Spring Boot (/actuator/prometheus)
      ↓  expone métricas en formato Prometheus
Prometheus
      ↓  raspa las métricas cada 15 segundos (pull model)
Grafana
      ↓  consulta Prometheus y grafica los datos
```

### Métricas disponibles

Spring Boot expone automáticamente métricas de:
- JVM (heap, non-heap, GC, threads)
- HTTP requests (rate, duración, errores por endpoint)
- HikariCP (conexiones activas, idle, pending)
- Logback (eventos por nivel: info, warn, error)
- Sistema (CPU, memoria del proceso)

Todas las métricas incluyen la etiqueta `application="taskflow-backend"` para identificar la fuente.

### Dashboard en Grafana

El dashboard JVM se provisiona automáticamente desde `docker/grafana/provisioning/dashboards/jvm-micrometer.json` — no hay que importarlo manualmente.

Al entrar a `http://localhost:3001` ve a **Dashboards → TaskFlow → JVM (Micrometer)**.

Para ver los datos selecciona:
- **Application:** `taskflow-backend`
- **Instance:** `backend:8080`

> **Nota:** Los paneles de I/O Overview se llenan al hacer requests reales a la API. Los paneles de JVM muestran datos desde el primer arranque.

### Verificar que Prometheus está raspando

En `http://localhost:9090` → **Status → Targets** debe aparecer `taskflow-backend` con estado **UP**.

## Logs

### Perfiles

| Perfil | Formato | Destino |
|---|---|---|
| `dev` | Texto plano con colores | Consola |
| `prod` | JSON estructurado | CloudWatch (via stdout) |

### Formato en producción (JSON)

En prod cada línea de log es un JSON válido que CloudWatch puede indexar y consultar:

```json
{
  "@timestamp": "2026-06-04T10:00:00.000Z",
  "level": "ERROR",
  "logger_name": "com.taskflow.auth.service.AuthService",
  "message": "Usuario no encontrado",
  "stack_trace": "...",
  "app": "taskflow",
  "env": "prod"
}
```

Esto permite filtrar en CloudWatch con queries como:
```
{ $.level = "ERROR" }
{ $.logger_name = "com.taskflow.auth*" }
```

### Retención de logs en AWS

Los logs se guardan en CloudWatch en el grupo `/ecs/taskflow` con retención de **7 días**. Al hacer `terraform destroy` el grupo se elimina junto con toda la infra.

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
| V8 | Usuario de prueba para k6 (k6test@taskflow.com) |

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
IAM              → Rol con OIDC para GitHub Actions (sin access keys estáticas)
```

### Levantar la infra

```bash
cd terraform

# Ver qué va a crear sin crear nada
terraform plan -var-file="terraform.tfvars"

# Crear toda la infra (~10 minutos, RDS es lo más lento)
terraform apply -var-file="terraform.tfvars"
```

Al terminar, Terraform imprime los outputs:
```
app_url     = "http://taskflow-alb-xxxxxxxxx.us-east-1.elb.amazonaws.com"
ecr_url     = "123456789.dkr.ecr.us-east-1.amazonaws.com/taskflow-backend"
```

### Bajar la infra

```bash
terraform destroy -var-file="terraform.tfvars"
```

> **Importante:** `terraform destroy` borra todo — RDS, ECS, ECR, VPC, Secrets Manager, CloudWatch. Los datos de la base de datos se pierden. Tarda ~5 minutos.

### Archivos de Terraform

| Archivo | Descripción |
|---|---|
| `main.tf` | Provider AWS y configuración base |
| `variables.tf` | Declaración de todas las variables |
| `vpc.tf` | Red — VPC, subnets públicas y privadas, NAT gateway |
| `ecr.tf` | Repositorio de imágenes Docker |
| `iam.tf` | Roles IAM para ECS (execution role y task role) |
| `secrets.tf` | Secrets Manager — JWT secret y contraseña de RDS |
| `rds.tf` | Base de datos PostgreSQL |
| `alb.tf` | Load balancer y security groups |
| `ecs.tf` | Cluster, task definition y servicio de ECS |
| `outputs.tf` | URLs y datos útiles al terminar el apply |
| `terraform.tfvars` | Valores reales (no va al repo — está en .gitignore) |

## CI/CD

| Rama | Pipeline | Acción |
|---|---|---|
| `develop` / PRs | `ci.yml` | Build, tests, análisis SonarCloud, reporte de cobertura |
| `prod` | `deploy.yml` | Build JAR → imagen Docker → push ECR → deploy ECS |

### Flujo de deploy a ECS

1. GitHub Actions se autentica con AWS via **OIDC** (sin access keys estáticas)
2. Build del JAR con Maven (sin tests — ya pasaron en CI)
3. Build y push de la imagen Docker a ECR (tag = git SHA + latest)
4. Descarga la Task Definition actual de ECS
5. Inyecta la nueva imagen en la Task Definition
6. Registra la nueva Task Definition y hace el deploy
7. Espera a que ECS confirme que el contenedor está healthy

### Secrets requeridos en GitHub

| Secret | Descripción |
|---|---|
| `AWS_ROLE_ARN` | ARN del rol IAM con permisos para ECR y ECS |
| `SONAR_TOKEN` | Token de autenticación de SonarCloud |
| `SONAR_PROJECT_KEY` | Clave del proyecto en SonarCloud |
| `SONAR_ORGANIZATION` | Organización en SonarCloud |

## Pruebas de carga (k6)

Los scripts están en `/k6`. Requiere [k6](https://k6.io) instalado.

### Instalar k6

```powershell
winget install k6 --source winget
```

### Correr la prueba

```bash
k6 run k6/load-test.js
```

### Escenarios del script

| Stage | Duración | VUs |
|---|---|---|
| Arranque | 30s | 50 |
| Carga alta | 1m | 200 |
| Carga muy alta | 1m | 500 |
| Límite | 1m | 1,000 |
| Bajada | 30s | 0 |

Los VUs (Virtual Users) son usuarios simultáneos simulados — cada uno ejecuta requests en loop independientemente.

### Resultados de referencia

Corrido en local con Docker Compose, PostgreSQL en contenedor:

| Configuración | p95 | Throughput | Errores |
|---|---|---|---|
| Pool default (10 conexiones) | 5.29s ❌ | 156 req/s | 0% |
| Pool optimizado (50 conexiones) | 2.19s ✅ | 328 req/s | 0% |

### Qué prueba el script

Cada VU ejecuta en loop:
1. `GET /notes` — listar notas paginadas
2. `POST /notes` — crear una nota
3. `GET /tags` — listar tags

Usa el usuario de prueba creado en V8 (`k6test@taskflow.com` / `K6TestPass123!`) con `email_verified = true` para login directo sin verificación de correo.

## Pool de conexiones Hikari

El pool se configura diferente por ambiente. La lógica: más conexiones = más throughput, pero cada conexión consume RAM en la DB.

| Perfil | `maximum-pool-size` | Por qué |
|---|---|---|
| `dev` (Docker) | 50 | Para pruebas de carga con k6 |
| base / `test` | 10 | Default seguro de Spring Boot |
| `prod` (ECS) | 5 | Fórmula HikariCP: `(vCPUs * 2) + discos` = `(2*2)+1` para db.t3.micro |

> **Referencia:** con pool=10 a 1,000 VUs el p95 es ~5.3s. Con pool=50 baja a ~2.2s y el throughput se duplica. En prod el límite real es `max_connections` de PostgreSQL (default 100) dividido entre las instancias de la app.

## Diferencia horaria

La región `us-east-1` (Norte de Virginia) tiene **1 hora más** que Ciudad de México en horario de verano (UTC-5 vs UTC-6). Los timestamps en CloudWatch aparecen en UTC — resta 5 o 6 horas según la época del año para convertir a hora local.
