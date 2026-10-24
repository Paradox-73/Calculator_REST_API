# syntax=docker/dockerfile:1

# ---------- Stage 1: build the executable JAR ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the POM first. Docker caches this layer, so a source-only change
# does not download the dependencies again.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: minimal runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user. The application never runs as root.
RUN addgroup -S spring && adduser -S -G spring spring

WORKDIR /app

# Copy only the JAR from the build stage. The Maven cache and the
# sources stay out of the final image.
COPY --from=build --chown=spring:spring /build/target/calculator-devops.jar /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# "exec" makes the JVM process PID 1, so it receives the stop signal.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
