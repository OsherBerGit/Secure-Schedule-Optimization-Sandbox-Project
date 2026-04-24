# ==========================================
# Stage 1: Build the application (Builder)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy only the pom.xml first to cache the dependencies download step
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the actual source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Create the secure runtime image
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# SECURITY: Create a non-root user and group to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the compiled JAR from the builder stage
# The JAR name is based on <artifactId> and <version> in your pom.xml
COPY --from=builder /build/target/main-backend-0.0.1-SNAPSHOT.jar ./app.jar

# Expose the port configured in application.properties
EXPOSE 8080

# Run the application with a secure random source (best practice for JWT/Cryptography)
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]