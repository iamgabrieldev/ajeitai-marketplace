# Stage 1: build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src
RUN sed -i 's/\r$//' mvnw 2>/dev/null || true && chmod +x mvnw && ./mvnw package -DskipTests -B

# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown -R app:app /app

USER app

EXPOSE 5000

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
