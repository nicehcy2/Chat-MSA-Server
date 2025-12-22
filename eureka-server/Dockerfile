# Build Stage
FROM gradle:8.4-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java","-jar","app.jar"]