# ----------- Stage 1: Build the JAR -----------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies first (cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ----------- Stage 2: Runtime Image -----------
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/unified-payment-middleware-backend.jar app.jar

# Create logs directory and set ownership
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=staging

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
