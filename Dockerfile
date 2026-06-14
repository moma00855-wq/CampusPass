# --- Stage 1: Build Stage ---
# Use an image that has Gradle 8.5 and JDK 17 pre-installed to bypass needing 'gradlew'
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# Switch to root to prevent permission issues on Render
USER root

# Copy all configuration and source files at once since we are bypassing caching layers 
# to ensure we capture folders like 'server', 'composeApp', 'backend', etc.
COPY . .

# Run the build task. 
# We use standard 'build' and skip tests to ensure it compiles whichever module contains your backend.
RUN gradle build -x test --no-daemon

# --- Stage 2: Production Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a secure non-root user for Render execution
RUN addgroup -S campusgroup && adduser -S campususer -G campusgroup
USER campususer

# The Magic Step: Use a recursive wildcard (**) to find the jar file wherever 
# Gradle compiled it (e.g., server/build/libs/ or build/libs/) and copy it over.
COPY --from=builder /app/**/build/libs/*-all.jar ./app.jar || \
    COPY --from=builder /app/**/build/libs/*.jar ./app.jar

# Expose the port your CampusPass application listens on
# (Render automatically maps this; adjust if your app runs on 8080, 5000, 3000, etc.)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
