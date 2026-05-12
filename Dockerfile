# ─────────────────────────────────────────────
# STAGE 1 — Build
# Compila el proyecto y genera el JAR
# Esta imagen NO va a producción — solo sirve para compilar
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar pom.xml primero — Docker cachea las dependencias
# Si solo cambia el código fuente, no re-descarga dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
# STAGE 2 — Runtime
# Solo contiene el JRE y el JAR — imagen más pequeña y segura
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Crear usuario no-root — buena práctica de seguridad
# Nunca correr la app como root en producción
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

WORKDIR /app

# Copiar solo el JAR del stage de build
COPY --from=builder /app/target/*.jar app.jar

# Cambiar propietario del archivo al usuario taskflow
RUN chown taskflow:taskflow app.jar

# Usar el usuario no-root
USER taskflow

# Puerto que expone la app — debe coincidir con server.port
EXPOSE 8080

# Variables de entorno con defaults seguros
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# Healthcheck — Docker verifica que la app esté viva
# Kubernetes usa su propio healthcheck via Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Comando de arranque
# $JAVA_OPTS permite pasar opciones JVM desde variables de entorno
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
