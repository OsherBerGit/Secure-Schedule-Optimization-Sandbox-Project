# ==========================================
# Stage 1: Build the application (Builder)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy only the main-backend files
COPY main-backend/pom.xml main-backend/
COPY main-backend/src main-backend/src/

# Build the application
RUN mvn -f main-backend/pom.xml clean package -DskipTests

# ==========================================
# Stage 2: Create the secure runtime image
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# SECURITY: Create a non-root user to run the application (Least Privilege principle)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the generated JAR from the build stage
COPY --from=builder /build/main-backend/target/main-backend-*.jar ./app.jar

# Expose the Main Backend port
EXPOSE 8080

# SECURITY: Use urandom to ensure a secure entropy source for Cryptography (BCrypt/JWT)
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]