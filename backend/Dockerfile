# Stage 1: Build the Spring Boot application
FROM amazoncorretto:21-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./mvnw package -DskipTests

# Stage 2: Runtime - Minimal image with JRE only
FROM amazoncorretto:21-alpine
WORKDIR /app

# Install tini for graceful shutdown and create non-root user
RUN apk add --no-cache tini && \
    addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from builder stage
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Expose port 8080 (Spring Boot default)
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Switch to non-root user
USER spring:spring

# Tini handles signal propagation (SIGTERM) for graceful shutdown
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]
