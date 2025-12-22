# Build Stage
FROM gradle:8.4-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

# Gradle cache only dependency stage
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle --no-daemon dependencies || true

# Copy source later (cache 유지)
COPY src src
RUN gradle --no-daemon build -x test

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java","-jar","app.jar"]