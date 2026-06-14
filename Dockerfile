# --- Stage 1: Build Stage ---
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 1. Copy the Gradle wrapper and configuration files first to leverage Docker caching
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./

# Grant execution rights on the Gradle wrapper and download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 2. Copy your actual Kotlin source code
COPY src ./src

# 3. Build the production application fat JAR (skipping tests for faster deployment)
RUN ./gradlew bootJar --no-daemon || ./gradlew shadowJar --no-daemon || ./gradlew build -x test --no-daemon

# --- Stage 2: Production Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S campusgroup && adduser -S campususer -G campusgroup
USER campususer

# Copy the built JAR file from the builder stage
# (Looks inside build/libs/ where Gradle outputs compiled JARs)
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the internal port your CampusPass app runs on (adjust if you use 8080, 5000, etc.)
EXPOSE 8080

# Run the Kotlin application
ENTRYPOINT ["java", "-jar", "app.jar"]
