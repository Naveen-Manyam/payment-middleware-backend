FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

ARG JAR_FILE=target/unified-payment-middleware-backend.jar
COPY ${JAR_FILE} app.jar

# ✅ Create logs directory and set correct ownership for non-root user
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=staging

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
