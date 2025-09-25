# ---------- Stage 1: Build JAR ----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven files and download dependencies first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy actual source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app
USER spring:spring

# Copy the JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Use environment variable to control Spring profile
ENV SPRING_PROFILES_ACTIVE=staging

ENTRYPOINT ["java", "-jar", "app.jar"]
